# Components

deephaven.ui provides many components to display in the UI. Use a component by assigning it to a variable, then display it in the UI.

```python
my_button = ui.button("Click Me!", on_press=lambda e: print(f"Button was clicked! {e}"))
```

Select any of the components below for API reference details. For examples and use cases, see the [examples]() page.

## Buttons

- [action_button]()
- [button]()
- [button_group]()
- [toggle_button]()

## Inputs

- [checkbox]()
- [form]()
- [number_field]()
- [picker]()
- [range_slider]()
- [slider]()
- [switch]()
- [text_field]()

## Content

- [content]()
- [contextual_help]()
- [flex]()
- [grid]()
- [heading]()
- [icon]()
- [item]()
- [illustrated_message]()
- [table]()
- [tabs]()
- [view]()

## Layout

- [column]()
- [row]()
- [dashboard]()
- [stack]()
- [panel]()
