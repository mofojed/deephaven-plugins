"""Local LLM agent widget for Deephaven.

A pure ``deephaven.ui`` widget that runs an agentic loop against a local LLM
(via Ollama). The agent can search the Deephaven docs, fetch external data, and
execute Deephaven Python code, displaying any tables/figures it creates in a
tabbed output panel next to the chat.

Example:
    from dh_agent import agent_chat

    chat = agent_chat()
"""

from __future__ import annotations

from typing import Any

from .config import AgentConfig, DEFAULT_CONFIG

# ``agent_chat`` is provided lazily via ``__getattr__`` below to avoid importing
# deephaven.ui (and starting the JVM) on package import.
__all__ = [
    "agent_chat",
    "AgentConfig",
    "DEFAULT_CONFIG",
]  # pyright: ignore[reportUnsupportedDunderAll]


def __getattr__(name: str) -> Any:
    # Lazy import so that importing submodules (e.g. for testing the agent
    # logic) does not require deephaven.ui to be installed.
    if name == "agent_chat":
        from .ui import agent_chat

        return agent_chat
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
