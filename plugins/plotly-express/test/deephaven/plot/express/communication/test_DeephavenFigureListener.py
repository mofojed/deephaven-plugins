"""Tests for the EVENT branch of DeephavenFigureListener.process_message.

The listener is a per-client MessageStream that already handles RETRIEVE
and FILTER. This file verifies the new EVENT message type dispatches to the
figure's registered Python handler and never drops the connection on
handler errors.
"""

from __future__ import annotations

import json
import unittest
from unittest.mock import MagicMock


class DeephavenFigureListenerEventsTestCase(unittest.TestCase):
    def _make_listener(self, handler):
        # Imported here so the module-level patches in BaseTest do not affect
        # other tests that do not need the figure machinery.
        import src.deephaven.plot.express as dx
        from src.deephaven.plot.express.communication.DeephavenFigureListener import (
            DeephavenFigureListener,
        )

        figure = dx.DeephavenFigure()
        figure.set_event_handler("click", handler)

        connection = MagicMock()
        listener = DeephavenFigureListener(figure, connection)
        return listener, figure, connection

    def test_event_message_dispatches_handler(self):
        captured_events = []
        listener, _, connection = self._make_listener(captured_events.append)

        payload = json.dumps(
            {
                "type": "EVENT",
                "event_type": "click",
                "data": {
                    "points": [{"curveNumber": 0, "pointIndex": 3, "location": "USA"}]
                },
            }
        ).encode()
        result_payload, result_refs = listener.process_message(payload, [])

        # EVENT is fire-and-forget — listener returns no payload and does not
        # send anything back through the connection for this branch.
        self.assertEqual(result_payload, b"")
        self.assertEqual(result_refs, [])

        self.assertEqual(len(captured_events), 1)
        event = captured_events[0]
        self.assertEqual(event.event_type, "click")
        self.assertEqual(event.points[0].location, "USA")
        self.assertEqual(event.points[0].point_index, 3)

    def test_event_handler_exception_does_not_break_listener(self):
        def raising_handler(_event):
            raise RuntimeError("intentional")

        listener, _, _ = self._make_listener(raising_handler)
        payload = json.dumps(
            {
                "type": "EVENT",
                "event_type": "click",
                "data": {"points": [{"curveNumber": 0}]},
            }
        ).encode()

        # Should not raise.
        result_payload, _ = listener.process_message(payload, [])
        self.assertEqual(result_payload, b"")


if __name__ == "__main__":
    unittest.main()
