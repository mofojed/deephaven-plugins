### deephaven.ui.button(\*children: Any, variant: Literal['accent', 'primary', 'secondary', 'negative', 'cta', 'overBackground'] | None = None, style: Literal['fill', 'outline'] | None = None, static_color: Literal['white', 'black'] | None = None, is_pending: bool | None = None, type: Literal['button', 'submit', 'reset'] = 'button', is_disabled: bool | None = None, auto_focus: bool | None = None, href: str | None = None, target: str | None = None, rel: str | None = None, element_type: Literal['div', 'button', 'a'] = 'button', on_press: Callable[[PressEvent], None] | None = None, on_press_start: Callable[[PressEvent], None] | None = None, on_press_end: Callable[[PressEvent], None] | None = None, on_press_up: Callable[[PressEvent], None] | None = None, on_press_change: Callable[[bool], None] | None = None, on_focus: Callable[[FocusEvent], None] | None = None, on_blur: Callable[[FocusEvent], None] | None = None, on_focus_change: Callable[[bool], None] | None = None, on_key_down: Callable[[KeyboardEvent], None] | None = None, on_key_up: Callable[[KeyboardEvent], None] | None = None, flex: str | int | float | bool | None = None, flex_grow: int | float | None = None, flex_shrink: int | float | None = None, flex_basis: str | int | float | None = None, align_self: Literal['auto', 'normal', 'start', 'end', 'center', 'flex-start', 'flex-end', 'self-start', 'self-end', 'stretch'] | None = None, justify_self: Literal['auto', 'normal', 'start', 'end', 'flex-start', 'flex-end', 'self-start', 'self-end', 'center', 'left', 'right', 'stretch'] | None = None, order: int | float | None = None, grid_area: str | None = None, grid_column: str | None = None, grid_row: str | None = None, grid_column_start: str | None = None, grid_column_end: str | None = None, grid_row_start: str | None = None, grid_row_end: str | None = None, margin: str | int | float | None = None, margin_top: str | int | float | None = None, margin_bottom: str | int | float | None = None, margin_start: str | int | float | None = None, margin_end: str | int | float | None = None, margin_x: str | int | float | None = None, margin_y: str | int | float | None = None, width: str | int | float | None = None, min_width: str | int | float | None = None, max_width: str | int | float | None = None, height: str | int | float | None = None, min_height: str | int | float | None = None, max_height: str | int | float | None = None, position: Literal['static', 'relative', 'absolute', 'fixed', 'sticky'] | None = None, top: str | int | float | None = None, bottom: str | int | float | None = None, left: str | int | float | None = None, right: str | int | float | None = None, start: str | int | float | None = None, end: str | int | float | None = None, z_index: int | float | None = None, is_hidden: bool | None = None, id: str | None = None, exclude_from_tab_order: bool | None = None, aria_expanded: Literal['true', 'false'] | bool | None = None, aria_has_popup: Literal['true', 'false'] | bool | Literal['menu', 'listbox', 'tree', 'grid', 'dialog'] | None = None, aria_controls: str | None = None, aria_pressed: Literal['true', 'false'] | bool | Literal['mixed'] | None = None, aria_label: str | None = None, aria_labelled_by: str | None = None, aria_described_by: str | None = None, aria_details: str | None = None, UNSAFE_class_name: str | None = None, UNSAFE_style: Dict[str, Any] | None = None)

Based on Spectrum’s [Button](https://react-spectrum.adobe.com/react-spectrum/Button.html) component.
Buttons allow users to perform an action or to navigate to another page. They have multiple styles for various needs, and are ideal for calling attention to where a user needs to do something in order to move forward in a flow.

Usage:

```python
my_button = ui.button("Click me!", on_press=lambda: print("Button clicked!"))
```

* **Parameters:**
  * **\*children** – The contents to display inside the button.
  * **variant** – The visual style of the button.
  * **style** – The background style of the button.
  * **static_color** – The static color style to apply. Useful when the button appears over a color background.
  * **is_pending** – Whether to disable events immediately and display a loading spinner after a 1 second delay.
  * **type** – The behavior of the button when used in an HTML form.
  * **is_disabled** – Whether the button is disabled.
  * **auto_focus** – Whether the button should automatically receive focus when the page loads.
  * **href** – A URL to link to if elementType=”a”.
  * **target** – The target window or tab to open the linked URL in.
  * **rel** – The relationship between the current document and the linked URL.
  * **on_press** – Function called when the button is pressed.
  * **on_press_start** – Function called when the button is pressed and held.
  * **on_press_end** – Function called when the button is released after being pressed.
  * **on_press_up** – Function called when the button is released.
  * **on_press_change** – Function called when the pressed state changes.
  * **on_focus** – Function called when the button receives focus.
  * **on_blur** – Function called when the button loses focus.
  * **on_focus_change** – Function called when the focus state changes.
  * **on_key_down** – Function called when a key is pressed down.
  * **on_key_up** – Function called when a key is released.
  * **flex** – When used in a flex layout, specifies how the element will grow or shrink to fit the space available.
  * **flex_grow** – When used in a flex layout, specifies how much the element will grow to fit the space available.
  * **flex_shrink** – When used in a flex layout, specifies how much the element will shrink to fit the space available.
  * **flex_basis** – When used in a flex layout, specifies the initial size of the element.
  * **align_self** – Overrides the alignItems property of a flex or grid container.
  * **justify_self** – Specifies how the element is justified inside a flex or grid container.
  * **order** – The layout for the element within a flex or grid container.
  * **grid_area** – The name of grid area to place the element in.
  * **grid_column** – The name of grid column to place the element in.
  * **grid_row** – The name of grid row to place the element in.
  * **grid_column_start** – The name of grid column to start the element in.
  * **grid_column_end** – The name of grid column to end the element in.
  * **grid_row_start** – The name of grid row to start the element in.
  * **grid_row_end** – The name of grid row to end the element in.
  * **margin** – The margin around the element.
  * **margin_top** – The margin above the element.
  * **margin_bottom** – The margin below the element.
  * **margin_start** – The margin for the logical start side of the element, depending on layout direction.
  * **margin_end** – The margin for the logical end side of the element, depending on layout direction.
  * **margin_x** – The margin for the horizontal sides of the element.
  * **margin_y** – The margin for the vertical sides of the element.
  * **width** – The width of the element.
  * **min_width** – The minimum width of the element.
  * **max_width** – The maximum width of the element.
  * **height** – The height of the element.
  * **min_height** – The minimum height of the element.
  * **max_height** – The maximum height of the element.
  * **position** – The position of the element.
  * **top** – The distance from the top of the containing element.
  * **bottom** – The distance from the bottom of the containing element.
  * **left** – The distance from the left of the containing element.
  * **right** – The distance from the right of the containing element.
  * **start** – The distance from the start of the containing element, depending on layout direction.
  * **end** – The distance from the end of the containing element, depending on layout direction.
  * **z_index** – The stack order of the element.
  * **is_hidden** – Whether the element is hidden.
  * **id** – The unique identifier of the element.
  * **exclude_from_tab_order** – Whether the element should be excluded from the tab order.
  * **aria_expanded** – Whether the element is expanded.
  * **aria_has_popup** – Whether the element has a popup.
  * **aria_controls** – The id of the element controlled by the current element.
  * **aria_pressed** – Whether the element is pressed.
  * **aria_label** – The label for the element.
  * **aria_labelled_by** – The id of the element that labels the current element.
  * **aria_described_by** – The id of the element that describes the current element.
  * **aria_details** – The details of the current element.
  * **UNSAFE_class_name** – A CSS class to apply to the element.
  * **UNSAFE_style** – A CSS style to apply to the element.
* **Returns:**
  The button element.
