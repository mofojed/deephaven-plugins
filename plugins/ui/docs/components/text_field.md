# ui.text_field

A text field is a single-line text input field. It is used to collect a single line of text from the user.

## Syntax

```py
from deephaven import ui

ui.text_field(
    value: str | NOne = None,
    label: str | None = None,
    label_position: LabelPosition | None = None
    on_change: Callable[[str], None] = None,
    **props,
) -> ui.Component
```

## Parameters

| Parameter        | Type                  | Description                                                                                            |
| ---------------- | --------------------- | ------------------------------------------------------------------------------------------------------ |
| `value`          | `str`                 | The initial value of the text field.                                                                   |
| `label`          | `str`                 | A label to display above the text field.                                                               |
| `label_position` | `LabelPosition`       | The position of the label. Can be `top` or `side`. Default is `top`.                                   |
| `on_change`      | Callable[[str], None] | A callback that is called when the text field's value changes. The new value is passed as an argument. |

## Examples

### Log value when text field changes

```python
from deephaven import ui

text_field = ui.text_field(
    placeholder="Enter your name", on_change=lambda e: print(f"Name changed to '{e}'")
)
```

![Value is changed to 'hello' and logged.](../assets/text_field_log.png)

### Display text field value in UI

```python
@ui.component
def ui_input():
    text, set_text = ui.use_state("hello")

    return [ui.text_field(value=text, on_change=set_text), ui.text(f"You typed {text}")]


my_input = ui_input()
```

![Value will update in the UI.](../assets/text_field_display_text.png)
