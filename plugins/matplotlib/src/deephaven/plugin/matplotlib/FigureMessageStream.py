from __future__ import annotations

from io import BytesIO
import logging
from typing import Any
from deephaven.plugin.object_type import MessageStream

from .figure_type import Figure

logger = logging.getLogger(__name__)

DPI = 144


class FigureMessageStream(MessageStream):
    _figure: Figure
    _is_closed: bool
    _is_exporting: bool

    def __init__(self, figure: Figure, connection: MessageStream):
        """
        Create a new FigureMessageStream. Just passes a table reference to the client for now.

        Args:
            figure: The figure to render
            connection: The connection to send the rendered element to
        """
        self._figure = figure
        self._connection = connection
        self._is_closed = False
        self._is_exporting = False
        self._count = 0
        print(f"FigureMessageStream created with figure: {figure}")

    def _handle_figure_update(self, artist, stale: bool) -> None:

        # Check if we're already drawing this figure, and the stale callback was triggered because of our call to savefig
        if self._is_exporting:
            # print("Figure is already exporting, skipping update")
            return

        import traceback

        traceback.print_stack()

        print("handle_figure_udpate", artist, stale)

        self._count += 1
        if self._count > 50:
            raise RuntimeError("Figure update callback called too many times")

        print("Figure update detected, exporting figure")
        self._is_exporting = True
        buf = BytesIO()

        print("Saving figure to buffer")
        self._figure.savefig(
            buf, format="PNG", dpi=DPI
        )  # Save the figure to the buffer
        self._connection.on_data(buf.getvalue(), [])
        self._figure.stale = False  # Reset the stale flag
        print("Figure saved to buffer and sent to client")

        self._is_exporting = False

    def start(self) -> None:
        """
        Start the message stream. All this does right now is send the table instance that AgGrid is wrapping to the client.
        If we added some options on AgGrid that we'd want to pass along to the client as well, we could serialize those as JSON options.
        """
        # Just send the table reference to the client
        print("Starting FigureMessageStream")

        # Send the initial figure to the client
        buf = BytesIO()
        print("Saving initial figure to buffer")
        self._figure.savefig(buf, format="PNG", dpi=DPI)
        self._connection.on_data(buf.getvalue(), [])
        print("Initial figure sent to client")
        self._figure.stale = False

        self._figure.stale_callback = self._handle_figure_update

    def on_close(self) -> None:
        assert not self._is_closed

        logger.debug("FigureMessageStream closed")
        print("FigureMessageStream closed")
        self._connection.on_close()
        self._is_closed = True
        del self._figure

    def on_data(self, payload: bytes, references: list[Any]) -> None:
        """
        Handle incoming data from the client. Right now we're not expecting any bidirectional communication for the AG Grid plugin.

        Args:
            payload: The payload from the client
            references: The references from the client
        """
        pass
