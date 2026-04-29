from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


EVENT_NAMES = (
    "click",
    "select",
    "deselect",
    "hover",
    "unhover",
    "relayout",
    "legend_click",
)

EVENT_HANDLER_KWARGS = tuple(f"on_{name}" for name in EVENT_NAMES)


@dataclass(frozen=True)
class PlotlyEventPoint:
    """A single point referenced by a plotly event.

    Fields are populated from the plotly point payload (see plotly's
    plotly_click / plotly_selected / plotly_hover events). Fields not present
    on a given chart type are ``None``.
    """

    curve_number: int
    point_index: int | None = None
    point_indices: list[int] | None = None
    x: Any | None = None
    y: Any | None = None
    z: Any | None = None
    lat: float | None = None
    lon: float | None = None
    location: str | None = None
    label: str | None = None
    value: Any | None = None
    hovertext: str | None = None
    customdata: Any | None = None
    raw: dict = field(default_factory=dict)

    @staticmethod
    def from_wire(point: dict) -> PlotlyEventPoint:
        return PlotlyEventPoint(
            curve_number=point.get("curveNumber", 0),
            point_index=point.get("pointIndex", point.get("pointNumber")),
            point_indices=point.get("pointIndices"),
            x=point.get("x"),
            y=point.get("y"),
            z=point.get("z"),
            lat=point.get("lat"),
            lon=point.get("lon"),
            location=point.get("location"),
            label=point.get("label"),
            value=point.get("value"),
            hovertext=point.get("hovertext"),
            customdata=point.get("customdata"),
            raw=point,
        )


@dataclass(frozen=True)
class PlotlyEvent:
    """A normalized plotly event delivered to a Python handler.

    ``event_type`` is one of :data:`EVENT_NAMES`. ``points`` is always a list;
    it is empty for events without point context (e.g. ``relayout``).
    """

    event_type: str
    points: list[PlotlyEventPoint] = field(default_factory=list)
    selection: dict | None = None
    relayout: dict | None = None
    legend: dict | None = None

    @staticmethod
    def from_wire(event_type: str, data: dict) -> PlotlyEvent:
        points = [PlotlyEventPoint.from_wire(p) for p in (data.get("points") or [])]
        return PlotlyEvent(
            event_type=event_type,
            points=points,
            selection=data.get("selection"),
            relayout=data.get("relayout"),
            legend=data.get("legend"),
        )
