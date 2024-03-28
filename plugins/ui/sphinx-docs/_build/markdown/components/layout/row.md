### deephaven.ui.row(\*children: Any, height: float | None = None, \*\*kwargs: Any)

A row is a container that can be used to group elements.
Each element will be placed to the right of its prior sibling. Use it along with [column](column) and [stack](stack) to group [panels](panel) together within a [dashboard](dashboard).

Usage:

```python
my_dashboard = ui.dashboard(
    ui.row(
        ui.panel("On your left!", title="Left Panel"),
        ui.panel("Right on!", title="Right Panel"),
    ),
)
```

* **Parameters:**
  * **children** – Elements to render in the row.
  * **height** – The percent height of the row relative to other children of its parent. If not provided, the row will be sized automatically.
* **Returns:**
  The row element.
