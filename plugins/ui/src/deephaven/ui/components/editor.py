from __future__ import annotations
from typing import Callable
from .basic import component_element
from .._internal.utils import create_props


def editor(
    *,
    value: str | None = None,
    default_value: str | None = None,
    on_change: Callable[[str], None] | None = None,
    language: str | None = None,
):
    """
    Create a new editor component.

    Args:
        value: The value of the editor.
        default_value: The default value of the editor.
        on_change: The function to call when the editor value changes.
        language: The language to use for syntax highlighting.

    Returns:
        The editor component.
    """
    children, props = create_props(locals())
    return component_element(
        "Editor",
        *children,
        **props,
    )
