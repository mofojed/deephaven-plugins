### deephaven.ui.column(\*children: Any, width: float | None = None, \*\*kwargs: Any)

A column is a container that can be used to group elements.
Each element will be placed below its prior sibling. Use it along with [row](row) and [stack](stack) to group [panels](panel) together within a [dashboard](dashboard).

Usage:

```python
my_dashboard = ui.dashboard(
    ui.column(
        ui.panel("Top of the world!", title="World"),
        ui.panel("In the valley below!", title="Valley"),
    ),
)
```

* **Parameters:**
  * **children** – Elements to render in the column.
  * **width** – The percent width of the column relative to other children of its parent. If not provided, the column will be sized automatically.
* **Returns:**
  The column element.
