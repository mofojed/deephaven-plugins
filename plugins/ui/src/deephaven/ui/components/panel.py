from __future__ import annotations

from typing import Any
from ..elements import BaseElement


def panel(*children: Any, title: str | None = None, **kwargs: Any):
    """
    A panel is a container that can be used to group elements. You can use it with `dashboard <dashboard>`_ to create a layout, or just use it ad-hoc to group components within a panel.

    Usage::

        my_panel = ui.panel("Panel content", title="Panel title")

    Args:
        children: Elements to render in the panel.
        title: Title of the panel.
    """
    return BaseElement(
        "deephaven.ui.components.Panel", *children, title=title, **kwargs
    )
