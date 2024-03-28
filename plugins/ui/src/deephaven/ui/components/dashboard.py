from __future__ import annotations

from typing import Any
from ..elements import DashboardElement, FunctionElement


def dashboard(element: FunctionElement):
    """
    A dashboard is the container for an entire layout.
    Use `columns <column>`_, `rows <row>`_, and `stacks <stack>`_ to group elements together within a dashboard.

    Usage::

        my_dashboard = ui.dashboard(
            ui.row(
                ui.column(
                    ui.panel("Top left panel", title="Top Left"),
                    ui.panel("Bottom left panel", title="Bottom Left"),
                ),
                ui.stack(
                    ui.panel("Right panel, stack 1", title="Right 1"),
                    ui.panel("Right panel, stack 2", title="Right 2"),
                )
            ),
        )

    Args:
        element: Element to render as the dashboard.
                 The element should render a layout that contains 1 root column or row.

    Returns:
        The dashboard element.
    """
    return DashboardElement(element)
