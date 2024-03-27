# Components

deephaven.ui provides many components to display in the UI. Use a component by assigning it to a variable, then display it in the UI.

```python
my_button = ui.button("Click Me!", on_press=lambda e: print(f"Button was clicked! {e}"))
```

Select any of the components below for API reference details. For examples and use cases, see the [examples](../examples) page.

## Buttons

<!-- TODO: How we get the toctree in .md here to show the buttons page? -->

```{toctree}
:maxdepth: 1
```

- [action_button](action_button.md)
- [button](button.md)
- [button_group](button_group.md)
- [toggle_button](toggle_button.md)

## Inputs

- [checkbox](checkbox.md)
- [form](form.md)
- [number_field](number_field.md)
- [picker](picker.md)
- [range_slider](range_slider.md)
- [slider](slider.md)
- [switch](switch.md)
- [text_field](text_field.md)

## Content

- [content](content.md)
- [contextual_help](contextual_help.md)
- [flex](flex.md)
- [grid](grid.md)
- [heading](heading.md)
- [icon](icon.md)
- [item](item.md)
- [illustrated_message](illustrated_message.md)
- [table](table.md)
- [tabs](tabs.md)
- [view](view.md)

## Layout

- [column](column.md)
- [row](row.md)
- [dashboard](dashboard.md)
- [stack](stack.md)
- [panel](panel.md)
