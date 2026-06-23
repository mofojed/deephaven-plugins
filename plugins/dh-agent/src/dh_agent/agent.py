"""Agent state and the agentic loop.

``AgentState`` is a thread-safe store of the conversation and output tabs. It is
created *outside* the ``@ui.component`` so the same state is shared across every
viewer of the widget (see the deephaven.ui re-render notes). The component
subscribes to it and re-renders when it changes.

``Agent`` drives the tool-calling loop against the LLM on a background thread so
the UI stays responsive.
"""

from __future__ import annotations

import logging
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

    def submit(self, user_text: str) -> None:
        """Handle a user message on a background thread."""
        if not user_text.strip() or self._state.busy:
            return
        thread = threading.Thread(target=self._run_turn, args=(user_text,), daemon=True)
        thread.start()

    def _run_turn(self, user_text: str) -> None:
        self._state.set_busy(True)
        try:
            self._state.add_message(ChatMessage(role="user", content=user_text))
            self._state.append_llm({"role": "user", "content": user_text})

            for _ in range(self._config.max_iterations):
                message = self._client.chat(
                    self._state.llm_history, tools=self._toolbox.schemas
                )
                self._state.append_llm(message)

                content = _get(message, "content")
                tool_calls = _get(message, "tool_calls") or []

                if content:
                    self._state.add_message(
                        ChatMessage(role="assistant", content=content)
                    )

                if not tool_calls:
                    break

                for call in tool_calls:
                    self._handle_tool_call(call)
        except Exception as exc:
            logger.exception("Agent turn failed")
            self._state.add_message(
                ChatMessage(role="error", content=f"Agent error: {exc}")
            )
        finally:
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


def _get(obj: Any, key: str) -> Any:
    """Read a field from either a mapping or an attribute-style object."""
    if isinstance(obj, Mapping):
        return obj.get(key)
    return getattr(obj, key, None)


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
