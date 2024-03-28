### deephaven.ui.flex(\*children: Any, direction: Literal['row', 'column', 'row-reverse', 'column-reverse'] | None = None, wrap: Literal['wrap', 'nowrap', 'wrap-reverse'] | None = None, justify_content: Literal['start', 'end', 'center', 'left', 'right', 'space-between', 'space-around', 'space-evenly', 'stretch', 'baseline', 'first baseline', 'last baseline', 'safe center', 'unsafe center'] | None = None, align_content: Literal['start', 'end', 'center', 'space-between', 'space-around', 'space-evenly', 'stretch', 'baseline', 'first baseline', 'last baseline', 'safe center', 'unsafe center'] | None = None, align_items: Literal['start', 'end', 'center', 'stretch', 'self-start', 'self-end', 'baseline', 'first baseline', 'last baseline', 'safe center', 'unsafe center'] | None = None, gap: str | int | float | None = 'size-100', column_gap: str | int | float | None = None, row_gap: str | int | float | None = None, \*\*props: Any)

Based on Spectrum’s [Flex](https://react-spectrum.adobe.com/react-spectrum/Flex.html) component.
Displays children in a flex layout.

Usage:

```python
my_flex = ui.flex(
    ui.text("Hello"),
    ui.text("World"),
    direction="column",
    gap="size-200",
)
```
