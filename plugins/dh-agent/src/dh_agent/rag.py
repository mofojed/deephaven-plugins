"""Lightweight retrieval-augmented generation (RAG) over Deephaven docs.

Indexes markdown files into chunks, embeds them with an Ollama embedding model,
and retrieves the most relevant chunks for a query via cosine similarity. The
index is cached to disk so it only has to be built once per docs revision.

This is intentionally dependency-light (numpy only) so it is easy to experiment
with. Swap in a real vector DB later if needed.
"""

from __future__ import annotations

import hashlib
import logging
import os
import pickle
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import numpy as np

from ._client import OllamaClient

logger = logging.getLogger(__name__)

_CHUNK_CHARS = 1200
_CHUNK_OVERLAP = 200


@dataclass
class DocChunk:
    source: str
    text: str


def _iter_markdown_files(roots: Iterable[Path]) -> Iterable[Path]:
    for root in roots:
        if root.is_file() and root.suffix.lower() in (".md", ".mdx", ".txt"):
            yield root
        elif root.is_dir():
            for ext in ("*.md", "*.mdx", "*.txt"):
                yield from root.rglob(ext)


def _chunk_text(text: str) -> list[str]:
    chunks: list[str] = []
    start = 0
    length = len(text)
    while start < length:
        end = min(start + _CHUNK_CHARS, length)
        chunks.append(text[start:end])
        if end == length:
            break
        start = end - _CHUNK_OVERLAP
    return chunks


def _cache_key(paths: list[Path], embed_model: str) -> str:
    hasher = hashlib.sha256()
    hasher.update(embed_model.encode())
    for path in sorted(paths):
        try:
            stat = path.stat()
            hasher.update(str(path).encode())
            hasher.update(str(stat.st_mtime_ns).encode())
        except OSError:
            continue
    return hasher.hexdigest()[:16]


class DocIndex:
    """An on-disk-cached embedding index over a set of docs directories."""

    def __init__(
        self,
        roots: Iterable[str | Path],
        client: OllamaClient,
        cache_dir: str | Path | None = None,
    ):
        self._roots = [Path(r) for r in roots]
        self._client = client
        self._cache_dir = Path(cache_dir or os.path.expanduser("~/.cache/dh_agent"))
        self._chunks: list[DocChunk] = []
        self._embeddings: np.ndarray | None = None

    def build(self, force: bool = False) -> None:
        """Build or load the index from cache."""
        files = sorted(set(_iter_markdown_files(self._roots)))
        if not files:
            logger.warning("No documentation files found under %s", self._roots)
            return

        self._cache_dir.mkdir(parents=True, exist_ok=True)
        cache_path = (
            self._cache_dir
            / f"index_{_cache_key(files, self._client.config.embed_model)}.pkl"
        )

        if cache_path.exists() and not force:
            with open(cache_path, "rb") as fh:
                data = pickle.load(fh)
            self._chunks = data["chunks"]
            self._embeddings = data["embeddings"]
            logger.info("Loaded RAG index with %d chunks from cache", len(self._chunks))
            return

        chunks: list[DocChunk] = []
        for path in files:
            try:
                text = path.read_text(encoding="utf-8", errors="ignore")
            except OSError:
                continue
            for chunk in _chunk_text(text):
                if chunk.strip():
                    chunks.append(DocChunk(source=str(path), text=chunk))

        if not chunks:
            return

        logger.info("Embedding %d documentation chunks...", len(chunks))
        vectors: list[list[float]] = []
        batch_size = 64
        for i in range(0, len(chunks), batch_size):
            batch = [c.text for c in chunks[i : i + batch_size]]
            vectors.extend(self._client.embed(batch))

        self._chunks = chunks
        self._embeddings = _normalize(np.array(vectors, dtype=np.float32))

        with open(cache_path, "wb") as fh:
            pickle.dump({"chunks": self._chunks, "embeddings": self._embeddings}, fh)
        logger.info("Built and cached RAG index with %d chunks", len(self._chunks))

    def search(self, query: str, top_k: int = 5) -> list[DocChunk]:
        """Return the most relevant doc chunks for the query."""
        if self._embeddings is None or not self._chunks:
            return []
        query_vec = _normalize(np.array(self._client.embed([query]), dtype=np.float32))
        scores = self._embeddings @ query_vec[0]
        top_idx = np.argsort(scores)[::-1][:top_k]
        return [self._chunks[i] for i in top_idx]


def _normalize(matrix: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(matrix, axis=-1, keepdims=True)
    norms[norms == 0] = 1.0
    return matrix / norms
