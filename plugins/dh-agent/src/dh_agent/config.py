"""Configuration for the Deephaven LLM agent.

All settings can be overridden via environment variables so the agent can be
pointed at different Ollama hosts / models without code changes.
"""

from __future__ import annotations

import os
from dataclasses import dataclass, field


def _env(name: str, default: str) -> str:
    value = os.environ.get(name)
    return value if value else default


@dataclass
class AgentConfig:
    """Runtime configuration for the agent."""

    # Ollama connection
    host: str = field(
        default_factory=lambda: _env("DH_AGENT_OLLAMA_HOST", "http://localhost:11434")
    )

    # The chat/agent model. Must support tool calling (tagged `tools` on ollama.com).
    model: str = field(
        default_factory=lambda: _env("DH_AGENT_MODEL", "qwen2.5-coder:7b")
    )

    # Embedding model used for retrieval-augmented generation (RAG).
    embed_model: str = field(
        default_factory=lambda: _env("DH_AGENT_EMBED_MODEL", "nomic-embed-text")
    )

    # Maximum agentic iterations (tool-call rounds) per user turn.
    max_iterations: int = field(
        default_factory=lambda: int(_env("DH_AGENT_MAX_ITERATIONS", "12"))
    )

    # Sampling temperature.
    temperature: float = field(
        default_factory=lambda: float(_env("DH_AGENT_TEMPERATURE", "0.2"))
    )

    # Number of RAG chunks to retrieve per search.
    rag_top_k: int = field(default_factory=lambda: int(_env("DH_AGENT_RAG_TOP_K", "5")))


DEFAULT_CONFIG = AgentConfig()
