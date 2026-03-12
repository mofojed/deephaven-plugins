from __future__ import annotations

import logging
from .BaseElement import BaseElement
from .FunctionElement import FunctionElement

logger = logging.getLogger(__name__)


class DashboardElement(BaseElement):
    def __init__(
        self,
        element: FunctionElement,
        /,
        show_close_icon: bool = False,
        show_headers: bool = True,
    ):
        super().__init__(
            "deephaven.ui.components.Dashboard",
            element,
            show_close_icon=show_close_icon,
            show_headers=show_headers,
        )
