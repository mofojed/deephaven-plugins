from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from .PlotlyEvent import PlotlyEvent, EVENT_NAMES

if TYPE_CHECKING:
    from ..deephaven_figure import DeephavenFigure


logger = logging.getLogger(__name__)


def dispatch(figure: "DeephavenFigure", event_type: str, data: dict) -> None:
    """Build a :class:`PlotlyEvent` from the wire payload and run the figure's
    registered handler for ``event_type``.

    Handler exceptions are logged and swallowed — a faulty handler must not
    drop the listener connection.
    """
    if event_type not in EVENT_NAMES:
        logger.warning("Ignoring unknown plotly event type: %s", event_type)
        return

    handler = figure.event_handlers.get(event_type)
    if handler is None:
        return

    event = PlotlyEvent.from_wire(event_type, data or {})
    try:
        handler(event)
    except Exception:
        logger.exception("Error in plotly event handler for '%s'", event_type)
