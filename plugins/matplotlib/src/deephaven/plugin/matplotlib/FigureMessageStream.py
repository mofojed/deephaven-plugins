from __future__ import annotations

from io import BytesIO
import logging
from typing import Any
from deephaven.plugin.object_type import MessageStream

from .figure_type import Figure

logger = logging.getLogger(__name__)

DPI = 144

import time
from functools import wraps


def throttle(seconds):
    def decorator(func):
        last_called = 0

        @wraps(func)
        def wrapper(*args, **kwargs):
            nonlocal last_called
            current_time = time.time()
            if current_time - last_called >= seconds:
                last_called = current_time
                return func(*args, **kwargs)

        return wrapper

    return decorator


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
        print(f"FigureMessageStream created with figure: {figure}")

    def _handle_figure_update(self, artist, stale: bool) -> None:
        # Check if we're already drawing this figure, and the stale callback was triggered because of our call to savefig
        if self._is_exporting:
            # print("Figure is already exporting, skipping update")
            return

        self._send_update()

        # self._is_exporting = True

        # # Print the stack trace
        # import traceback

        # # print("Figure update detected, handling update")
        # traceback.print_stack()

        # print("Figure update detected, exporting figure")
        # buf = BytesIO()

        # print("Saving figure to buffer")
        # self._figure.savefig(
        #     buf, format="PNG", dpi=DPI
        # )  # Save the figure to the buffer
        # self._connection.on_data(buf.getvalue(), [])
        # print("Figure saved to buffer and sent to client")

        # self._is_exporting = False

    @throttle(0.5)  # Throttle updates to every 0.5 seconds
    def _send_update(self) -> None:
        """
        Send an update to the client. This is throttled to avoid sending too many updates in a short period of time.
        """
        if self._is_exporting:
            return
        self._is_exporting = True

        buf = BytesIO()

        print("Saving figure to buffer")
        self._figure.savefig(
            buf, format="PNG", dpi=DPI
        )  # Save the figure to the buffer
        self._connection.on_data(buf.getvalue(), [])
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

        self._figure.stale_callback = self._handle_figure_update

    def on_close(self) -> None:
        assert not self._is_closed

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
