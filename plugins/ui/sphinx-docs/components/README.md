# Components

deephaven.ui provides many components to display in the UI. Use a component by assigning it to a variable, then display it in the UI.

```python
my_button = ui.button("Click Me!", on_press=lambda e: print(f"Button was clicked! {e}"))
```

Select any of the components below for API reference details. For examples and use cases, see the [examples](../examples) page.

## Buttons

- [action_button](action_button)
- [button](button)
- [button_group](button_group)
- [toggle_button](toggle_button)

## Inputs

- [checkbox](checkbox)
- [form](form)
- [number_field](number_field)
- [picker](picker)
- [range_slider](range_slider)
- [slider](slider)
- [switch](switch)
- [text_field](text_field)

## Content

- [content](content)
- [contextual_help](contextual_help)
- [flex](flex)
- [grid](grid)
- [heading](heading)
- [icon](icon)
- [item](item)
- [illustrated_message](illustrated_message)
- [table](table)
- [tabs](tabs)
- [view](view)

## Layout

- [column](column)
- [row](row)
- [dashboard](dashboard)
- [stack](stack)
- [panel](panel)
