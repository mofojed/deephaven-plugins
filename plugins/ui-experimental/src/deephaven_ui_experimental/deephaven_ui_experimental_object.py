from __future__ import annotations
from typing import Callable

from deephaven.plugin.object_type import MessageStream


class DeephavenUiExperimentalObject:
    """
    This is a simple object that demonstrates how to send messages to the client.
    When the object is created, it will be passed a connection to the client.
    This connection can be used to send messages back to the client.
    """

    _connection: MessageStream
    """
    The connection to the client
    """

    _on_change: Callable[[str], None]
    """
    The callback to call when the datetime changes
    """

    _default_value: str
    """
    The default value for the datetime.
    Of the format YYYY-MM-DD HH:MM:SS.sssssssss. Uses the user's timezone.

    Can just fill in a partial date as well. For example, "2021-01" is a valid default value, and the remaining fields will be filled in with the first of each value, e.g. "2021-01-01 00:00:00.000000000".
    """

    def __init__(self, on_change: Callable[[str], None], default_value: str = ""):
        self._connection: MessageStream = None
        self._on_change = on_change
        self._default_value = default_value

    def send_message(self, message: str) -> None:
        """
        Send a message to the client

        Args:
            message: The message to send
        """
        if self._connection:
            self._connection.send_message(message)

    def _set_connection(self, connection: MessageStream) -> None:
        """
        Set the connection to the client.
        This is called on the object when it is created.

        Args:
            connection: The connection to the client
        """
        self._connection = connection

    def on_change(self, value: str) -> None:
        """
        Handle a change in the datetime

        Args:
            value: The new value
        """
        self._on_change(value)

    @property
    def default_value(self) -> str | None:
        """
        Get the default value

        Returns:
            The default value
        """
        return self._default_value
