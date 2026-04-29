# Event Handlers

Plotly-express plots can run a Python callable in response to user
interactions on the chart — clicking a country on a map, selecting a
range on a scatter plot, clicking a category in a bar chart, and so on.
Handlers are registered as keyword arguments on the plot function:

| Keyword | Plotly event | Use it for |
| --- | --- | --- |
| `on_click` | `plotly_click` | A point/marker/category was clicked |
| `on_select` | `plotly_selected` | A box or lasso selection was finalized |
| `on_deselect` | `plotly_deselect` | The user cleared a selection |
| `on_hover` | `plotly_hover` | The cursor moved over a point |
| `on_unhover` | `plotly_unhover` | The cursor left a point |
| `on_relayout` | `plotly_relayout` | Axis ranges, dragmode, or layout changed |
| `on_legend_click` | `plotly_legendclick` | A legend entry was clicked |

Each handler receives a single `dx.PlotlyEvent` argument. Handlers run
synchronously on the per-client listener thread, so a slow handler
temporarily blocks subsequent messages from that one client only.
Exceptions raised by a handler are logged and swallowed — they cannot
break the chart connection.

## Example: react to a country click on a map

```python skip-test
import deephaven.plot.express as dx
from deephaven import new_table
from deephaven.column import string_col, double_col

countries = new_table([
    string_col("iso3", ["USA", "CAN", "MEX"]),
    double_col("gdp", [21.4, 1.6, 1.3]),
])

def handle_click(event: dx.PlotlyEvent) -> None:
    point = event.points[0]
    print(f"Clicked {point.location} (curve {point.curve_number}, index {point.point_index})")

choropleth = dx.scatter_geo(
    countries,
    locations="iso3",
    color="gdp",
    on_click=handle_click,
)
```

## Example: react to a bar-chart category click

```python skip-test
import deephaven.plot.express as dx

stocks = dx.data.stocks()

def handle_bar_click(event: dx.PlotlyEvent) -> None:
    point = event.points[0]
    print(f"Bar clicked: x={point.x}, y={point.y}")

bar = dx.bar(stocks, x="Sym", y="Price", on_click=handle_bar_click)
```

## The event object

`PlotlyEvent` carries normalized fields populated from the Plotly
event payload. Fields that are not present on a given chart type are
`None`.

```python skip-test
@dataclass(frozen=True)
class PlotlyEventPoint:
    curve_number: int
    point_index: int | None
    point_indices: list[int] | None  # box/lasso select
    x: Any | None
    y: Any | None
    z: Any | None
    lat: float | None
    lon: float | None
    location: str | None    # iso3 / state / etc. on geo plots
    label: str | None       # pie / sunburst / treemap
    value: Any | None       # pie / sunburst / indicator
    hovertext: str | None
    customdata: Any | None
    raw: dict               # full sanitized point dict (escape hatch)

@dataclass(frozen=True)
class PlotlyEvent:
    event_type: str         # "click" | "select" | ... | "legend_click"
    points: list[PlotlyEventPoint]
    selection: dict | None  # for "select"
    relayout: dict | None   # for "relayout"
    legend: dict | None     # for "legend_click"
```

Use `point.raw` if you need a Plotly point field that the dataclass
does not surface; fields outside the safety whitelist (`data`,
`fullData`, axis refs, etc.) are dropped before transmission.

## Multi-client behavior

Each viewer of a plotly-express figure has their own connection to the
server. If two users have the same figure open and both click, the
handler fires twice — once per click, on each client's listener
thread. The figure object holds the handler set; all viewers see the
same registered events.

## Limitations

- Handlers are fire-and-forget. v1 does not propagate handler return
  values back to the JS client; you cannot mutate the figure
  from inside a handler and have the change reflected automatically.
  Use a deephaven Table and Deephaven UI for that.
- High-frequency events (`hover`, `unhover`) are not throttled.
- Composing figures via `dx.layer` or `make_subplots` does not bubble
  events from inner figures; register handlers on the leaf
  plot-function call.
