import logging
import unittest
from unittest.mock import MagicMock

from deephaven.plot.express.events import dispatch


class FakeFigure:
    def __init__(self, handlers):
        self.event_handlers = handlers


class TestEventDispatcher(unittest.TestCase):
    def test_dispatch_runs_handler(self):
        handler = MagicMock()
        figure = FakeFigure({"click": handler})

        dispatch(figure, "click", {"points": [{"curveNumber": 0, "pointIndex": 1}]})

        handler.assert_called_once()
        event = handler.call_args[0][0]
        self.assertEqual(event.event_type, "click")
        self.assertEqual(len(event.points), 1)
        self.assertEqual(event.points[0].point_index, 1)

    def test_dispatch_no_handler_is_noop(self):
        figure = FakeFigure({})
        # Should not raise
        dispatch(figure, "click", {"points": []})

    def test_dispatch_swallows_handler_exception(self):
        def bad_handler(_event):
            raise ValueError("boom")

        figure = FakeFigure({"click": bad_handler})
        # Must not propagate; exception is logged.
        with self.assertLogs(
            "deephaven.plot.express.events.EventDispatcher", level="ERROR"
        ):
            dispatch(figure, "click", {"points": [{"curveNumber": 0}]})

    def test_dispatch_unknown_event_type_logs_warning(self):
        handler = MagicMock()
        figure = FakeFigure({"click": handler})
        with self.assertLogs(
            "deephaven.plot.express.events.EventDispatcher", level="WARNING"
        ):
            dispatch(figure, "not_a_real_event", {})
        handler.assert_not_called()


if __name__ == "__main__":
    unittest.main()
