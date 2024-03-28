### deephaven.ui.dashboard(element: FunctionElement)

A dashboard is the container for an entire layout.
Use [columns](column), [rows](row), and [stacks](stack) to group elements together within a dashboard.

Usage:

```python
my_dashboard = ui.dashboard(
    ui.row(
        ui.column(
            ui.panel("Top left panel", title="Top Left"),
            ui.panel("Bottom left panel", title="Bottom Left"),
        ),
        ui.stack(
            ui.panel("Right panel, stack 1", title="Right 1"),
            ui.panel("Right panel, stack 2", title="Right 2"),
        ),
    ),
)
```

* **Parameters:**
  **element** – Element to render as the dashboard.
  The element should render a layout that contains 1 root column or row.
* **Returns:**
  The dashboard element.
