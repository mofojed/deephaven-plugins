"""Access to the vendored Deephaven query-writing agent skill.

The ``deephaven-core-query-writing`` skill (Apache-2.0, by Deephaven Data Labs)
is bundled under ``skills/``. Its ``SKILL.md`` body is injected into the system
prompt, and the deep-dive files under ``references/`` are exposed to the model
through the ``read_skill_reference`` tool so it can pull the relevant guidance
before writing code.
"""

from __future__ import annotations

import os
from pathlib import Path

_SKILL_ROOT = (
    Path(__file__).resolve().parent / "skills" / "deephaven-core-query-writing"
)
_REFERENCES_DIR = _SKILL_ROOT / "references"


def _strip_frontmatter(text: str) -> str:
    """Remove a leading YAML frontmatter block (``--- ... ---``) if present."""
    if not text.startswith("---"):
        return text
    end = text.find("\n---", 3)
    if end == -1:
        return text
    newline = text.find("\n", end + 1)
    if newline == -1:
        return ""
    return text[newline + 1 :]


def load_skill_body() -> str:
    """Return the SKILL.md content with its frontmatter removed.

    Returns an empty string if the skill file is missing so the agent can still
    operate (with a thinner prompt).
    """
    try:
        text = (_SKILL_ROOT / "SKILL.md").read_text(encoding="utf-8")
    except OSError:
        return ""
    return _strip_frontmatter(text).strip()


def list_references() -> list[str]:
    """Return the available reference names (file stems, without ``.md``)."""
    try:
        return sorted(p.stem for p in _REFERENCES_DIR.glob("*.md"))
    except OSError:
        return []


def read_reference(name: str) -> str:
    """Return the text of a named reference.

    ``name`` may be given with or without the ``.md`` extension. Path
    components are stripped to prevent directory traversal; unknown names
    return a helpful message listing the valid options.
    """
    stem = os.path.basename(str(name)).strip()
    if stem.endswith(".md"):
        stem = stem[:-3]
    available = list_references()
    if not stem or stem not in available:
        return (
            f"Unknown reference '{name}'. "
            f"Available references: {', '.join(available) or '(none)'}."
        )
    return (_REFERENCES_DIR / f"{stem}.md").read_text(encoding="utf-8")
