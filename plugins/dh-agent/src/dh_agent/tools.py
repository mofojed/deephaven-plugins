"""Tool definitions and dispatch for the Deephaven agent.

Tools are described to the model using the Ollama / OpenAI function-calling
schema. The :class:`ToolBox` executes a requested tool call and returns a string
result that is fed back to the model.
"""

from __future__ import annotations

import json
import logging
from typing import Any, Callable, Mapping

from .executor import CapturedOutput, CodeExecutor

logger = logging.getLogger(__name__)


# JSON schemas advertised to the model.
TOOL_SCHEMAS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "run_deephaven_code",
            "description": (
                "Execute Python code in the live Deephaven session. Variables "
                "persist across calls. Any table or figure assigned to a "
                "top-level variable is shown to the user in a new tab. Returns "
                "stdout, created object names, and any error/traceback."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "code": {
                        "type": "string",
                        "description": "The Deephaven Python code to execute.",
                    }
                },
                "required": ["code"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_docs",
            "description": (
                "Search the Deephaven documentation for APIs, syntax, and "
                "examples. Use this before writing code you are unsure about."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "What to look up in the Deephaven docs.",
                    }
                },
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "fetch_url",
            "description": (
                "Fetch the text content of a web page or HTTP API. Use to "
                "gather external data or reference information."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "url": {
                        "type": "string",
                        "description": "The absolute http(s) URL to fetch.",
                    }
                },
                "required": ["url"],
            },
        },
    },
]


class ToolBox:
    """Dispatches model tool calls to their implementations."""

    def __init__(
        self,
        executor: CodeExecutor,
        on_outputs: Callable[[list[CapturedOutput]], None] | None = None,
        doc_search: Callable[[str], str] | None = None,
    ):
        self._executor = executor
        self._on_outputs = on_outputs
        self._doc_search = doc_search

    @property
    def schemas(self) -> list[dict[str, Any]]:
        return TOOL_SCHEMAS

    def dispatch(self, name: str, arguments: Mapping[str, Any] | str) -> str:
        """Run the named tool with the given arguments and return text output."""
        args = _coerce_args(arguments)
        handler = {
            "run_deephaven_code": self._run_deephaven_code,
            "search_docs": self._search_docs,
            "fetch_url": self._fetch_url,
        }.get(name)
        if handler is None:
            return f"Unknown tool: {name}"
        try:
            return handler(args)
        except Exception as exc:  # pragma: no cover - defensive
            logger.exception("Tool %s failed", name)
            return f"Tool {name} raised an error: {exc}"

    def _run_deephaven_code(self, args: Mapping[str, Any]) -> str:
        code = str(args.get("code", ""))
        if not code.strip():
            return "No code provided."
        result = self._executor.execute(code)
        if result.outputs and self._on_outputs is not None:
            self._on_outputs(result.outputs)
        return result.to_model_text()

    def _search_docs(self, args: Mapping[str, Any]) -> str:
        query = str(args.get("query", ""))
        if self._doc_search is None:
            return "Documentation search is not available in this session."
        result = self._doc_search(query)
        return result or "No relevant documentation found."

    def _fetch_url(self, args: Mapping[str, Any]) -> str:
        url = str(args.get("url", ""))
        if not url.startswith(("http://", "https://")):
            return "Only absolute http(s) URLs are supported."
        import httpx  # type: ignore[import-not-found]

        try:
            response = httpx.get(url, timeout=20, follow_redirects=True)
            response.raise_for_status()
        except Exception as exc:
            return f"Failed to fetch {url}: {exc}"
        text = response.text
        # Truncate to keep the context manageable.
        limit = 8000
        if len(text) > limit:
            text = text[:limit] + "\n... [truncated]"
        return text


def _coerce_args(arguments: Mapping[str, Any] | str) -> Mapping[str, Any]:
    if isinstance(arguments, str):
        try:
            return json.loads(arguments)
        except json.JSONDecodeError:
            return {}
    return arguments
