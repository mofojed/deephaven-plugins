from __future__ import annotations

from typing import Any
from ..elements import BaseElement


def row(*children: Any, height: float | None = None, **kwargs: Any):
    """
    A row is a container that can be used to group elements.
    Each element will be placed to the right of its prior sibling. Use it along with `column <column>`_ and `stack <stack>`_ to group `panels <panel>`_ together within a `dashboard <dashboard>`_.

    Usage::

        my_dashboard = ui.dashboard(
            ui.row(
                ui.panel("On your left!", title="Left Panel"),
                ui.panel("Right on!", title="Right Panel"),
            ),
        )

    Args:
        children: Elements to render in the row.
        height: The percent height of the row relative to other children of its parent. If not provided, the row will be sized automatically.

    Returns:
        The row element.
    """
    return BaseElement(
        "deephaven.ui.components.Row", *children, height=height, **kwargs
    )
