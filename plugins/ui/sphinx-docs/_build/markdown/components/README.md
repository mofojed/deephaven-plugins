# Components

deephaven.ui provides many components to display in the UI. Use a component by assigning it to a variable, then display it in the UI.

```python
my_button = ui.button("Click Me!", on_press=lambda e: print(f"Button was clicked! {e}"))
```

Select any of the components below for API reference details. For examples and use cases, see the [examples]() page.

## Buttons

Button components are used to trigger actions via user interaction:

* [`action_button()`](buttons/action_button.md)
* [`button()`](buttons/button.md)
* [`button_group()`](buttons/button_group.md)
* [`toggle_button()`](buttons/toggle_button.md)

## Inputs

Get input from the user with input components:

* [`checkbox()`](inputs/checkbox.md)
* [`form()`](inputs/form.md)
* [`number_field()`](inputs/number_field.md)
* [`picker()`](inputs/picker.md)
* [`range_slider()`](inputs/range_slider.md)
* [`slider()`](inputs/slider.md)
* [`switch()`](inputs/switch.md)
* [`text_field()`](inputs/text_field.md)

## Content

Display content in the UI with content components:

* [`content()`](content/content.md)
* [`contextual_help()`](content/contextual_help.md)
* [`flex()`](content/flex.md)
* [`grid()`](content/grid.md)
* [`heading()`](content/heading.md)
* [`icon()`](content/icon.md)
* [`illustrated_message()`](content/illustrated_message.md)
* [`item()`](content/item.md)
* [`table()`](content/table.md)
* [`tabs()`](content/tabs.md)
* [`tab_panels()`](content/tabs.md#deephaven.ui.tab_panels)
* [`tab_list()`](content/tabs.md#deephaven.ui.tab_list)
* [`view()`](content/view.md)

## Layout

Organize components in the UI with layout components:

* [`column()`](layout/column.md)
* [`dashboard()`](layout/dashboard.md)
* [`panel()`](layout/panel.md)
* [`row()`](layout/row.md)
* [`stack()`](layout/stack.md)
