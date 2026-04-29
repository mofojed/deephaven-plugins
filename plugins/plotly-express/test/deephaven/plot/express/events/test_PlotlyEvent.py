import unittest

from deephaven.plot.express.events import PlotlyEvent, PlotlyEventPoint


class TestPlotlyEvent(unittest.TestCase):
    def test_from_wire_click_geo(self):
        data = {
            "points": [
                {
                    "curveNumber": 0,
                    "pointNumber": 14,
                    "pointIndex": 14,
                    "lat": 39.5,
                    "lon": -98.35,
                    "location": "USA",
                    "hovertext": "United States",
                    "customdata": ["USA", 21000000],
                }
            ]
        }
        event = PlotlyEvent.from_wire("click", data)

        self.assertEqual(event.event_type, "click")
        self.assertEqual(len(event.points), 1)
        point = event.points[0]
        self.assertEqual(point.curve_number, 0)
        self.assertEqual(point.point_index, 14)
        self.assertEqual(point.location, "USA")
        self.assertAlmostEqual(point.lat, 39.5)
        self.assertAlmostEqual(point.lon, -98.35)
        self.assertEqual(point.customdata, ["USA", 21000000])
        self.assertEqual(point.hovertext, "United States")
        self.assertIsNone(event.selection)
        self.assertIsNone(event.relayout)
        self.assertIsNone(event.legend)

    def test_from_wire_select_with_indices(self):
        data = {
            "points": [
                {"curveNumber": 0, "pointIndex": 1, "x": 10, "y": 20},
                {"curveNumber": 0, "pointIndex": 4, "x": 11, "y": 21},
            ],
            "selection": {"range": {"x": [0, 100], "y": [0, 100]}},
        }
        event = PlotlyEvent.from_wire("select", data)

        self.assertEqual(event.event_type, "select")
        self.assertEqual(len(event.points), 2)
        self.assertEqual(event.selection, {"range": {"x": [0, 100], "y": [0, 100]}})
        self.assertEqual(event.points[0].x, 10)
        self.assertEqual(event.points[1].point_index, 4)

    def test_from_wire_relayout(self):
        data = {"relayout": {"xaxis.range[0]": 0, "xaxis.range[1]": 1}}
        event = PlotlyEvent.from_wire("relayout", data)

        self.assertEqual(event.event_type, "relayout")
        self.assertEqual(event.points, [])
        self.assertEqual(event.relayout, {"xaxis.range[0]": 0, "xaxis.range[1]": 1})

    def test_from_wire_legend_click(self):
        data = {"legend": {"curveNumber": 2, "visible": "legendonly"}}
        event = PlotlyEvent.from_wire("legend_click", data)

        self.assertEqual(event.event_type, "legend_click")
        self.assertEqual(event.legend, {"curveNumber": 2, "visible": "legendonly"})

    def test_from_wire_falls_back_to_pointNumber(self):
        # plotly_click events on some chart types only have pointNumber
        point = PlotlyEventPoint.from_wire({"curveNumber": 0, "pointNumber": 7})
        self.assertEqual(point.point_index, 7)

    def test_from_wire_empty_points(self):
        event = PlotlyEvent.from_wire("hover", {})
        self.assertEqual(event.event_type, "hover")
        self.assertEqual(event.points, [])

    def test_raw_preserves_full_payload(self):
        wire_point = {
            "curveNumber": 1,
            "pointIndex": 0,
            "extraField": "should be in raw",
        }
        point = PlotlyEventPoint.from_wire(wire_point)
        self.assertEqual(point.raw, wire_point)


if __name__ == "__main__":
    unittest.main()
