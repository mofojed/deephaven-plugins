"""Thin wrapper around the Ollama Python client.

Keeps the rest of the codebase decoupled from the exact client API and provides
a single place to inject the host / model configuration.
"""

from __future__ import annotations

import logging
from typing import Any, Iterable, Mapping, Sequence

from .config import AgentConfig, DEFAULT_CONFIG

logger = logging.getLogger(__name__)


class OllamaClient:
    """Wraps ``ollama.Client`` for chat + embeddings."""

    def __init__(self, config: AgentConfig = DEFAULT_CONFIG):
        # Imported lazily so that importing this package does not hard-require
        # ollama to be installed until the agent is actually used.
        import ollama  # type: ignore[import-not-found]

        self._config = config
        self._client = ollama.Client(host=config.host)
        self._model = config.model

    @property
    def config(self) -> AgentConfig:
        return self._config

    @property
    def model(self) -> str:
        """The chat model currently in use."""
        return self._model

    def set_model(self, model: str) -> None:
        """Switch the chat model used for subsequent requests."""
        if model:
            self._model = model

    def list_models(self) -> list[str]:
        """Return the names of locally available Ollama models, sorted.

        Excludes the configured embedding model. Returns an empty list if the
        server cannot be reached.
        """
        try:
            result = self._client.list()
        except Exception as exc:  # pragma: no cover - network dependent
            logger.warning("Could not list Ollama models: %s", exc)
            return []
        if isinstance(result, Mapping):
            raw = result.get("models", [])
        else:
            raw = getattr(result, "models", [])
        names: list[str] = []
        for item in raw:
            if isinstance(item, Mapping):
                name = item.get("model") or item.get("name")
            else:
                name = getattr(item, "model", None) or getattr(item, "name", None)
            if name and name != self._config.embed_model:
                names.append(str(name))
        return sorted(set(names))

    def chat(
        self,
        messages: Sequence[Mapping[str, Any]],
        tools: Sequence[Mapping[str, Any]] | None = None,
    ) -> Mapping[str, Any]:
        """Send a chat request and return the assistant message.

        Args:
            messages: Full conversation history in Ollama message format.
            tools: Optional list of tool/function schemas the model may call.

        Returns:
            The ``message`` portion of the response, a mapping with ``content``
            and optionally ``tool_calls``.
        """
        response = self._client.chat(
            model=self._model,
            messages=list(messages),
            tools=list(tools) if tools else None,
            options={"temperature": self._config.temperature},
        )
        # ollama >= 0.3 returns an object with attribute access and dict access.
        message = response["message"]
        return message

    def embed(self, texts: Iterable[str]) -> list[list[float]]:
        """Return embedding vectors for the given texts."""
        result = self._client.embed(model=self._config.embed_model, input=list(texts))
        return list(result["embeddings"])

    def is_available(self) -> bool:
        """Return True if the Ollama server is reachable."""
        try:
            self._client.list()
            return True
        except Exception as exc:  # pragma: no cover - network dependent
            logger.warning(
                "Ollama server not reachable at %s: %s", self._config.host, exc
            )
            return False
