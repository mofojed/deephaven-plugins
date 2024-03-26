### deephaven.ui.action_button(\*children: Any, type: Literal['button', 'submit', 'reset'] = 'button', on_press: Callable[[PressEvent], None] | None = None, on_press_start: Callable[[PressEvent], None] | None = None, on_press_end: Callable[[PressEvent], None] | None = None, on_press_up: Callable[[PressEvent], None] | None = None, on_press_change: Callable[[bool], None] | None = None, on_focus: Callable[[FocusEvent], None] | None = None, on_blur: Callable[[FocusEvent], None] | None = None, on_focus_change: Callable[[bool], None] | None = None, on_key_down: Callable[[KeyboardEvent], None] | None = None, on_key_up: Callable[[KeyboardEvent], None] | None = None, auto_focus: bool | None = None, is_disabled: bool | None = None, is_quiet: bool | None = None, static_color: Literal['white', 'black'] | None = None, flex: str | int | float | bool | None = None, flex_grow: int | float | None = None, flex_shrink: int | float | None = None, flex_basis: str | int | float | None = None, align_self: Literal['auto', 'normal', 'start', 'end', 'center', 'flex-start', 'flex-end', 'self-start', 'self-end', 'stretch'] | None = None, justify_self: Literal['auto', 'normal', 'start', 'end', 'flex-start', 'flex-end', 'self-start', 'self-end', 'center', 'left', 'right', 'stretch'] | None = None, order: int | float | None = None, grid_area: str | None = None, grid_row: str | None = None, grid_row_start: str | None = None, grid_row_end: str | None = None, grid_column: str | None = None, grid_column_start: str | None = None, grid_column_end: str | None = None, margin: str | int | float | None = None, margin_top: str | int | float | None = None, margin_bottom: str | int | float | None = None, margin_start: str | int | float | None = None, margin_end: str | int | float | None = None, margin_x: str | int | float | None = None, margin_y: str | int | float | None = None, width: str | int | float | None = None, height: str | int | float | None = None, min_width: str | int | float | None = None, min_height: str | int | float | None = None, max_width: str | int | float | None = None, max_height: str | int | float | None = None, position: Literal['static', 'relative', 'absolute', 'fixed', 'sticky'] | None = None, top: str | int | float | None = None, bottom: str | int | float | None = None, start: str | int | float | None = None, end: str | int | float | None = None, left: str | int | float | None = None, right: str | int | float | None = None, z_index: int | float | None = None, is_hidden: bool | None = None, id: str | None = None, exclude_from_tab_order: bool | None = None, aria_expanded: Literal['true', 'false'] | bool | None = None, aria_haspopup: Literal['true', 'false'] | bool | Literal['menu', 'listbox', 'tree', 'grid', 'dialog'] | None = None, aria_controls: str | None = None, aria_label: str | None = None, aria_labelledby: str | None = None, aria_describedby: str | None = None, aria_pressed: Literal['true', 'false'] | bool | Literal['mixed'] | None = None, aria_details: str | None = None, UNSAFE_class_name: str | None = None, UNSAFE_style: Dict[str, Any] | None = None)

ActionButtons allow users to perform an action. They’re used for similar, task-based options within a workflow, and are ideal for interfaces where buttons aren’t meant to draw a lot of attention.
Python implementation for the Adobe React Spectrum ActionButton component: [https://react-spectrum.adobe.com/react-spectrum/ActionButton.html](https://react-spectrum.adobe.com/react-spectrum/ActionButton.html)

* **Parameters:**
  * **\*children** – The content to display inside the button.
  * **type** – The type of button to render. (default: “button”)
  * **on_press** – Function called when the button is pressed.
  * **on_press_start** – Function called when the button is pressed.
  * **on_press_end** – Function called when a press interaction ends, either over the target or when the pointer leaves the target.
  * **on_press_up** – Function called when the button is released.
  * **on_press_change** – Function called when the press state changes.
  * **on_focus** – Function called when the button receives focus.
  * **on_blur** – Function called when the button loses focus.
  * **on_focus_change** – Function called when the focus state changes.
  * **on_key_down** – Function called when a key is pressed.
  * **on_key_up** – Function called when a key is released.
  * **auto_focus** – Whether the button should automatically get focus when the page loads.
  * **is_disabled** – Whether the button is disabled.
  * **is_quiet** – Whether the button should be quiet.
  * **static_color** – The static color style to apply. Useful when the button appears over a color background.
  * **flex** – When used in a flex layout, specifies how the element will grow or shrink to fit the space available.
  * **flex_grow** – When used in a flex layout, specifies how much the element will grow to fit the space available.
  * **flex_shrink** – When used in a flex layout, specifies how much the element will shrink to fit the space available.
  * **flex_basis** – When used in a flex layout, specifies the initial size of the element.
  * **align_self** – Overrides the align_items property of a flex or grid container.
  * **justify_self** – Specifies how the element is justified inside a flex or grid container.
  * **order** – The layout order for the element within a flex or grid container.
  * **grid_area** – The name of the grid area to place the element in.
  * **grid_row** – The name of the grid row to place the element in.
  * **grid_row_start** – The name of the grid row to start the element in.
  * **grid_row_end** – The name of the grid row to end the element in.
  * **grid_column** – The name of the grid column to place the element in.
  * **grid_column_start** – The name of the grid column to start the element in.
  * **grid_column_end** – The name of the grid column to end the element in.
  * **margin** – The margin to apply around the element.
  * **margin_top** – The margin to apply above the element.
  * **margin_bottom** – The margin to apply below the element.
  * **margin_start** – The margin to apply before the element.
  * **margin_end** – The margin to apply after the element.
  * **margin_x** – The margin to apply to the left and right of the element.
  * **margin_y** – The margin to apply to the top and bottom of the element.
  * **width** – The width of the element.
  * **height** – The height of the element.
  * **min_width** – The minimum width of the element.
  * **min_height** – The minimum height of the element.
  * **max_width** – The maximum width of the element.
  * **max_height** – The maximum height of the element.
  * **position** – Specifies how the element is positioned.
  * **top** – The distance from the top of the containing element.
  * **bottom** – The distance from the bottom of the containing element.
  * **start** – The distance from the start of the containing element.
  * **end** – The distance from the end of the containing element.
  * **left** – The distance from the left of the containing element.
  * **right** – The distance from the right of the containing element.
  * **z_index** – The stack order of the element.
  * **is_hidden** – Whether the element is hidden.
  * **id** – A unique identifier for the element.
  * **exclude_from_tab_order** – Whether the element should be excluded from the tab order.
  * **aria_expanded** – Whether the element is expanded.
  * **aria_haspopup** – Whether the element has a popup.
  * **aria_controls** – The id of the element that the element controls.
  * **aria_label** – The label for the element.
  * **aria_labelledby** – The id of the element that labels the element.
  * **aria_describedby** – The id of the element that describes the element.
  * **aria_pressed** – Whether the element is pressed.
  * **aria_details** – The details for the element.
  * **UNSAFE_class_name** – A CSS class to apply to the element.
  * **UNSAFE_style** – A CSS style to apply to the element.
