"""Agent state and the agentic loop.

``AgentState`` is a thread-safe store of the conversation and output tabs. It is
created *outside* the ``@ui.component`` so the same state is shared across every
viewer of the widget (see the deephaven.ui re-render notes). The component
subscribes to it and re-renders when it changes.

``Agent`` drives the tool-calling loop against the LLM on a background thread so
the UI stays responsive.
"""

from __future__ import annotations

import json
import logging
import re
import threading
from dataclasses import dataclass, field
from typing import Any, Callable, Mapping

from ._client import OllamaClient
from .config import AgentConfig, DEFAULT_CONFIG
from .executor import CapturedOutput, CodeExecutor
from .prompts import SYSTEM_PROMPT
from .tools import ToolBox

logger = logging.getLogger(__name__)

Listener = Callable[[], None]

# Tools the model may call. Used to recognise text-encoded tool calls emitted
# by models that do not support native function calling.
_KNOWN_TOOLS = {
    "run_deephaven_code",
    "search_docs",
    "read_skill_reference",
    "fetch_url",
}

# Matches fenced code blocks, optionally tagged ``python``/``py``. Used as a
# fallback for local models that emit code in markdown instead of calling the
# ``run_deephaven_code`` tool.
_CODE_BLOCK_RE = re.compile(
    r"```(?P<lang>[a-zA-Z0-9_+-]*)[ \t]*\r?\n(?P<code>.*?)```",
    re.DOTALL,
)
_NON_PYTHON_LANGS = {
    "bash",
    "sh",
    "shell",
    "zsh",
    "console",
    "text",
    "json",
    "yaml",
    "yml",
}

# Matches any fenced block (used to pull out JSON tool-call payloads).
_FENCE_RE = re.compile(r"```[a-zA-Z0-9_+-]*[ \t]*\r?\n(?P<body>.*?)```", re.DOTALL)


@dataclass
class ChatMessage:
    """A message shown in the chat panel."""

    role: str  # "user" | "assistant" | "tool" | "error" | "status"
    content: str
    tool_name: str | None = None
    tool_args: str | None = None


@dataclass
class OutputTab:
    """A renderable object shown in the output panel."""

    key: str
    title: str
    value: Any
    kind: str


class AgentState:
    """Thread-safe conversation + output store with change notifications."""

    def __init__(self) -> None:
        self._lock = threading.RLock()
        self._messages: list[ChatMessage] = []
        self._outputs: list[OutputTab] = []
        self._llm_history: list[Any] = [{"role": "system", "content": SYSTEM_PROMPT}]
        self._busy = False
        self._listeners: list[Listener] = []
        self._output_counter = 0

    # -- subscription -------------------------------------------------------
    def subscribe(self, listener: Listener) -> Callable[[], None]:
        with self._lock:
            self._listeners.append(listener)

        def unsubscribe() -> None:
            with self._lock:
                if listener in self._listeners:
                    self._listeners.remove(listener)

        return unsubscribe

    def _notify(self) -> None:
        with self._lock:
            listeners = list(self._listeners)
        for listener in listeners:
            try:
                listener()
            except Exception:  # pragma: no cover - defensive
                logger.exception("Agent state listener failed")

    # -- reads (return copies) ---------------------------------------------
    @property
    def messages(self) -> list[ChatMessage]:
        with self._lock:
            return list(self._messages)

    @property
    def outputs(self) -> list[OutputTab]:
        with self._lock:
            return list(self._outputs)

    @property
    def busy(self) -> bool:
        with self._lock:
            return self._busy

    @property
    def llm_history(self) -> list[Any]:
        with self._lock:
            return list(self._llm_history)

    # -- writes -------------------------------------------------------------
    def set_busy(self, busy: bool) -> None:
        with self._lock:
            self._busy = busy
        self._notify()

    def add_message(self, message: ChatMessage) -> None:
        with self._lock:
            self._messages.append(message)
        self._notify()

    def append_llm(self, message: Any) -> None:
        with self._lock:
            self._llm_history.append(message)

    def add_outputs(self, captured: list[CapturedOutput]) -> None:
        with self._lock:
            for output in captured:
                self._output_counter += 1
                self._outputs.append(
                    OutputTab(
                        key=f"output-{self._output_counter}",
                        title=output.name,
                        value=output.value,
                        kind=output.kind,
                    )
                )
        self._notify()


class Agent:
    """Drives the LLM tool-calling loop."""

    def __init__(
        self,
        state: AgentState,
        client: OllamaClient,
        toolbox: ToolBox,
        config: AgentConfig = DEFAULT_CONFIG,
    ):
        self._state = state
        self._client = client
        self._toolbox = toolbox
        self._config = config
        self._cancel_event = threading.Event()

    def submit(self, user_text: str) -> None:
        """Handle a user message on a background thread."""
        if not user_text.strip() or self._state.busy:
            return
        self._cancel_event.clear()
        thread = threading.Thread(target=self._run_turn, args=(user_text,), daemon=True)
        thread.start()

    def cancel(self) -> None:
        """Request cancellation of the in-progress turn.

        The running turn stops at the next checkpoint (before the next model
        call or tool execution). An in-flight model/tool call is allowed to
        finish, but its result is not acted upon further.
        """
        if self._state.busy:
            self._cancel_event.set()

    def _run_turn(self, user_text: str) -> None:
        self._state.set_busy(True)
        try:
            self._state.add_message(ChatMessage(role="user", content=user_text))
            self._state.append_llm({"role": "user", "content": user_text})

            for _ in range(self._config.max_iterations):
                if self._cancel_event.is_set():
                    break
                message = self._client.chat(
                    self._state.llm_history, tools=self._toolbox.schemas
                )
                if self._cancel_event.is_set():
                    break
                self._state.append_llm(message)

                content = _get(message, "content") or ""
                tool_calls = _get(message, "tool_calls") or []
                logger.info(
                    "Agent iteration: %d tool_call(s), %d chars of content",
                    len(tool_calls),
                    len(content),
                )

                if content:
                    self._state.add_message(
                        ChatMessage(role="assistant", content=content)
                    )

                if tool_calls:
                    for call in tool_calls:
                        if self._cancel_event.is_set():
                            break
                        self._handle_tool_call(call)
                    continue

                # Fallback 1: some models describe the tool call as a JSON
                # object (often in a ```json fence) instead of using native
                # function calling. Dispatch those.
                text_tool_calls = _extract_tool_calls_from_text(content)
                if text_tool_calls:
                    logger.info(
                        "No native tool calls; dispatching %d JSON tool call(s)",
                        len(text_tool_calls),
                    )
                    for name, args in text_tool_calls:
                        if self._cancel_event.is_set():
                            break
                        self._run_fallback_tool(name, args)
                    continue

                # Fallback 2: many coder models simply write code in a markdown
                # fence. Execute those blocks so the agentic loop still works.
                code_blocks = _extract_python_code(content)
                if code_blocks:
                    logger.info(
                        "No native tool calls; executing %d fenced code block(s)",
                        len(code_blocks),
                    )
                    for code in code_blocks:
                        if self._cancel_event.is_set():
                            break
                        self._run_fallback_tool("run_deephaven_code", {"code": code})
                    continue

                break
        except Exception as exc:
            logger.exception("Agent turn failed")
            self._state.add_message(
                ChatMessage(role="error", content=f"Agent error: {exc}")
            )
        finally:
            if self._cancel_event.is_set():
                self._state.add_message(
                    ChatMessage(role="status", content="Stopped by user.")
                )
            self._state.set_busy(False)

    def _handle_tool_call(self, call: Any) -> None:
        function = _get(call, "function") or {}
        name = _get(function, "name") or ""
        arguments = _get(function, "arguments")

        self._state.add_message(
            ChatMessage(
                role="tool",
                content="",
                tool_name=name,
                tool_args=_format_args(arguments),
            )
        )

        result = self._toolbox.dispatch(name, arguments)

        self._state.add_message(
            ChatMessage(role="tool", content=result, tool_name=name)
        )
        self._state.append_llm({"role": "tool", "content": result, "tool_name": name})

    def _run_fallback_tool(self, name: str, arguments: Mapping[str, Any]) -> None:
        """Dispatch a tool call the model expressed in its text content.

        Used when the model wrote a JSON tool call or a code block instead of
        emitting a native tool call. The result is fed back as a user message
        so the conversation stays valid for models without tool support.
        """
        self._state.add_message(
            ChatMessage(
                role="tool",
                content="",
                tool_name=name,
                tool_args=_format_args(arguments),
            )
        )
        result = self._toolbox.dispatch(name, arguments)
        self._state.add_message(
            ChatMessage(role="tool", content=result, tool_name=name)
        )
        self._state.append_llm(
            {
                "role": "user",
                "content": f"Result of `{name}`:\n{result}",
            }
        )


def _get(obj: Any, key: str) -> Any:
    """Read a field from either a mapping or an attribute-style object."""
    if isinstance(obj, Mapping):
        return obj.get(key)
    return getattr(obj, key, None)


def _extract_python_code(content: str) -> list[str]:
    """Return executable code blocks from markdown ``content``.

    Blocks tagged with a clearly non-Python language (e.g. ``bash``) are
    skipped; untagged and ``python``/``py`` blocks are returned.
    """
    if not content:
        return []
    blocks: list[str] = []
    for match in _CODE_BLOCK_RE.finditer(content):
        lang = match.group("lang").strip().lower()
        if lang in _NON_PYTHON_LANGS:
            continue
        code = match.group("code").strip()
        if code:
            blocks.append(code)
    return blocks


def _extract_tool_calls_from_text(content: str) -> list[tuple[str, Mapping[str, Any]]]:
    """Find tool calls a model encoded as JSON in its text content.

    Handles a JSON object (or list) either fenced in a code block or making up
    the whole message, in shapes like ``{"name": ..., "arguments": {...}}`` or
    ``{"function": {"name": ..., "arguments": {...}}}``. Only calls naming a
    known tool are returned, to avoid treating arbitrary JSON output as a call.
    """
    if not content or "{" not in content:
        return []
    candidates: list[str] = [
        m.group("body").strip() for m in _FENCE_RE.finditer(content)
    ]
    stripped = content.strip()
    if stripped.startswith("{") or stripped.startswith("["):
        candidates.append(stripped)

    calls: list[tuple[str, Mapping[str, Any]]] = []
    for candidate in candidates:
        obj = _try_load_json(candidate)
        if obj is None:
            continue
        for name, args in _normalize_tool_objects(obj):
            if name in _KNOWN_TOOLS:
                calls.append((name, args))
    return calls


def _try_load_json(text: str) -> Any:
    try:
        return json.loads(text)
    except Exception:
        return None


def _normalize_tool_objects(obj: Any) -> list[tuple[str, Mapping[str, Any]]]:
    items = obj if isinstance(obj, list) else [obj]
    out: list[tuple[str, Mapping[str, Any]]] = []
    for item in items:
        if not isinstance(item, Mapping):
            continue
        function = item.get("function")
        source = function if isinstance(function, Mapping) else item
        name = source.get("name") or item.get("tool") or item.get("tool_name")
        args: Any = source.get("arguments")
        if args is None:
            args = source.get("parameters")
        if isinstance(args, str):
            args = _try_load_json(args) or {}
        if isinstance(name, str) and isinstance(args, Mapping):
            out.append((name, args))
    return out


def _format_args(arguments: Any) -> str:
    if arguments is None:
        return ""
    if isinstance(arguments, str):
        return arguments
    import json

    try:
        return json.dumps(dict(arguments), indent=2)
    except Exception:
        return str(arguments)
