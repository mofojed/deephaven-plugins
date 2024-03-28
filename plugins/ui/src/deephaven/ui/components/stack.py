from __future__ import annotations

from typing import Any
from ..elements import BaseElement
from .._internal.utils import create_props


def stack(
    *children: Any,
    height: float | None = None,
    width: float | None = None,
    active_item_index: int | None = None,
    **props: Any,
):
    """
    A stack is a container that can be used to group elements which creates a set of tabs.
    Each element will get a tab and only one element can be visible at a time.
    Use it within a `dashboard <dashboard>`_ to group `panels <panel>`_ together.

    Usage::

        my_dashboard = ui.dashboard(
            ui.stack(
                ui.panel("Tab 1 content", title="Tab 1"),
                ui.panel("Tab 2 content", title="Tab 2"),
                active_item_index=0
            )
        )

    Args:
        children: Elements to render in the row.
        height: The percent height of the stack relative to other children of its parent. If not provided, the stack will be sized automatically.
        width: The percent width of the stack relative to other children of its parent. If not provided, the stack will be sized automatically.
        active_item_index: The index of the active item in the stack. If not provided, the last item will be active.

    Returns:
        The stack element.
    """
    children, props = create_props(locals())
    return BaseElement(
        "deephaven.ui.components.Stack",
        *children,
        **props,
    )
