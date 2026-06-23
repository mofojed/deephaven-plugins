"""Fallback documentation lookup against deephaven.io.

When the local (vendored skill) RAG index has no strong match for a query, the
agent can reach out to the public docs at ``deephaven.io``. The docs site serves
every page as clean Markdown via a ``.md`` URL, so we:

1. Run a keyless site-restricted web search (DuckDuckGo HTML endpoint) to find
   the most relevant doc page URLs.
2. Fetch the top page's Markdown and return a truncated excerpt.

Everything here is stdlib + ``httpx`` (already a dependency) and is imported
lazily so the JVM-side import of this package stays cheap.
"""

from __future__ import annotations

import logging
import re
import urllib.parse

logger = logging.getLogger(__name__)

_DDG_HTML = "https://html.duckduckgo.com/html/"
_DOCS_SITE = "deephaven.io/core/docs"
_USER_AGENT = "Mozilla/5.0 (compatible; dh-agent)"


def _as_markdown_url(url: str) -> str:
    """Normalize a docs page URL to its raw Markdown (``.md``) variant."""
    base = url.split("#", 1)[0].split("?", 1)[0]
    if base.endswith(".md"):
        return base
    return base.rstrip("/") + ".md"


def _parse_ddg_urls(html: str) -> list[str]:
    """Extract deephaven.io docs result URLs from a DuckDuckGo HTML response."""
    urls: list[str] = []
    for raw in re.findall(r"uddg=([^&\"']+)", html):
        decoded = urllib.parse.unquote(raw)
        if _DOCS_SITE in decoded and decoded not in urls:
            urls.append(decoded)
    return urls


def search_deephaven_io(
    query: str,
    *,
    max_chars: int = 6000,
    max_pages: int = 3,
    timeout: float = 20.0,
) -> str:
    """Search deephaven.io docs and return the top page's Markdown excerpt.

    Returns an empty string if the search fails or nothing relevant is found.
    """
    query = query.strip()
    if not query:
        return ""

    import httpx  # type: ignore[import-not-found]

    headers = {"User-Agent": _USER_AGENT}
    try:
        resp = httpx.get(
            _DDG_HTML,
            params={"q": f"site:{_DOCS_SITE} {query}"},
            headers=headers,
            timeout=timeout,
            follow_redirects=True,
        )
        resp.raise_for_status()
    except Exception as exc:  # noqa: BLE001 - network best-effort
        logger.warning("deephaven.io search failed: %s", exc)
        return ""

    urls = _parse_ddg_urls(resp.text)
    for url in urls[:max_pages]:
        md_url = _as_markdown_url(url)
        try:
            page = httpx.get(
                md_url, headers=headers, timeout=timeout, follow_redirects=True
            )
            page.raise_for_status()
        except Exception as exc:  # noqa: BLE001 - try the next candidate
            logger.debug("failed to fetch %s: %s", md_url, exc)
            continue

        text = page.text.strip()
        if not text:
            continue
        if len(text) > max_chars:
            text = text[:max_chars] + "\n\n... [truncated]"
        return f"From deephaven.io documentation ({url}):\n\n{text}"

    return ""
