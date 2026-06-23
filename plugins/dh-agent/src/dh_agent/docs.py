"""Documentation search orchestration.

Combines the local (vendored skill) RAG index with a deephaven.io web fallback.
This module is deliberately free of any ``deephaven.ui`` import so the routing
logic can be unit-tested without a running server.
"""

from __future__ import annotations

import os
from typing import Callable, Optional, Protocol

from . import web_docs

_NO_RESULTS = "No relevant documentation found."


class _Index(Protocol):
    """Minimal interface required from a RAG index."""

    def build(self) -> None:
        ...

    def search_scored(self, query: str, top_k: int = ...) -> list:
        ...


def make_doc_search(
    index: Optional[_Index],
    *,
    web_fallback: bool = True,
    min_score: float = 0.62,
    top_k: int = 5,
    web_searcher: Callable[[str], str] = web_docs.search_deephaven_io,
) -> Optional[Callable[[str], str]]:
    """Build a ``doc_search(query) -> str`` callable.

    The returned callable searches the local ``index`` first. If the best local
    match is weaker than ``min_score`` (or there is no local index), it falls
    back to ``web_searcher`` (deephaven.io). Returns ``None`` when neither a
    local index nor the web fallback is available.
    """
    if index is None and not web_fallback:
        return None

    built = {"done": False}

    def doc_search(query: str) -> str:
        local_text = ""
        best_score = 0.0

        if index is not None:
            if not built["done"]:
                try:
                    index.build()
                finally:
                    built["done"] = True
            scored = index.search_scored(query, top_k=top_k)
            if scored:
                best_score = scored[0][1]
                local_text = "\n\n---\n\n".join(
                    f"[{os.path.basename(chunk.source)}]\n{chunk.text}"
                    for chunk, _ in scored
                )

        # Fall back to deephaven.io when the local match is weak/missing.
        if web_fallback and best_score < min_score:
            web_text = web_searcher(query)
            if web_text:
                return web_text

        if local_text:
            return local_text
        return _NO_RESULTS

    return doc_search
