from __future__ import annotations

from typing import Any
from ..elements import BaseElement


def fragment(*children: Any):
    """
    A React.Fragment: https://react.dev/reference/react/Fragment.
    Used to group elements together without a wrapper node.

    Usage::
        my_fragment = ui.fragment(
            ui.text("Hello"),
            ui.text("World"),
        )

    Args:
        children: The children in the fragment.
    """
    return BaseElement("deephaven.ui.components.Fragment", children=children)
