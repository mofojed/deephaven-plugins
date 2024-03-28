### deephaven.ui.panel(\*children: Any, title: str | None = None, \*\*kwargs: Any)

A panel is a container that can be used to group elements. You can use it with [dashboard](dashboard) to create a layout, or just use it ad-hoc to group components within a panel.

Usage:

```python
my_panel = ui.panel("Panel content", title="Panel title")
```

* **Parameters:**
  * **children** – Elements to render in the panel.
  * **title** – Title of the panel.
