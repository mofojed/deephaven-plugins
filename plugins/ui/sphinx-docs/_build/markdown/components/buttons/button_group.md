### deephaven.ui.button_group(\*children: Any, is_disabled: bool | None = None, orientation: Literal['horizontal', 'vertical'] = 'horizontal', alignment: Literal['auto', 'normal', 'start', 'end', 'center', 'flex-start', 'flex-end', 'self-start', 'self-end', 'stretch'] = 'start', flex: str | int | float | bool | None = None, flex_grow: int | float | None = None, flex_shrink: int | float | None = None, flex_basis: str | int | float | None = None, align_self: Literal['auto', 'normal', 'start', 'end', 'center', 'flex-start', 'flex-end', 'self-start', 'self-end', 'stretch'] | None = None, justify_self: Literal['auto', 'normal', 'start', 'end', 'flex-start', 'flex-end', 'self-start', 'self-end', 'center', 'left', 'right', 'stretch'] | None = None, order: int | float | None = None, grid_area: str | None = None, grid_row: str | None = None, grid_column: str | None = None, grid_row_start: str | None = None, grid_row_end: str | None = None, grid_column_start: str | None = None, grid_column_end: str | None = None, margin: str | int | float | None = None, margin_top: str | int | float | None = None, margin_bottom: str | int | float | None = None, margin_start: str | int | float | None = None, margin_end: str | int | float | None = None, margin_x: str | int | float | None = None, margin_y: str | int | float | None = None, width: str | int | float | None = None, height: str | int | float | None = None, min_width: str | int | float | None = None, min_height: str | int | float | None = None, max_width: str | int | float | None = None, max_height: str | int | float | None = None, position: Literal['static', 'relative', 'absolute', 'fixed', 'sticky'] | None = None, top: str | int | float | None = None, bottom: str | int | float | None = None, left: str | int | float | None = None, right: str | int | float | None = None, start: str | int | float | None = None, end: str | int | float | None = None, z_index: int | float | None = None, is_hidden: bool | None = None, id: str | None = None, UNSAFE_class_name: str | None = None, UNSAFE_style: Dict[str, Any] | None = None)

A button group is a grouping of button whose actions are related to each other.

* **Parameters:**
  * **\*children** – The children of the button group.
  * **is_disabled** – Whether the button group is disabled.
  * **orientation** – The axis the ButtonGroup should align with. Setting this to ‘vertical’ will prevent any switching behaviours between ‘vertical’ and horizontal’.
  * **alignment** – The alignment of the buttons within the ButtonGroup.
  * **flex** – When used in a flex layout, specifies how the element will grow or shrink to fit the space available.
  * **flex_grow** – When used in a flex layout, specifies how the element will grow to fit the space available.
  * **flex_shrink** – When used in a flex layout, specifies how the element will shrink to fit the space available.
  * **flex_basis** – When used in a flex layout, specifies the initial main size of the element.
  * **align_self** – Overrides the alignItems property of a flex or grid container.
  * **justify_self** – Species how the element is justified inside a flex or grid container.
  * **order** – The layout order for the element within a flex or grid container.
  * **grid_area** – When used in a grid layout specifies, specifies the named grid area that the element should be placed in within the grid.
  * **grid_row** – When used in a grid layout, specifies the row the element should be placed in within the grid.
  * **grid_column** – When used in a grid layout, specifies the column the element should be placed in within the grid.
  * **grid_row_start** – When used in a grid layout, specifies the starting row to span within the grid.
  * **grid_row_end** – When used in a grid layout, specifies the ending row to span within the grid.
  * **grid_column_start** – When used in a grid layout, specifies the starting column to span within the grid.
  * **grid_column_end** – When used in a grid layout, specifies the ending column to span within the grid.
  * **margin** – The margin for all four sides of the element.
  * **margin_top** – The margin for the top side of the element.
  * **margin_bottom** – The margin for the bottom side of the element.
  * **margin_start** – The margin for the logical start side of the element, depending on layout direction.
  * **margin_end** – The margin for the logical end side of the element, depending on layout direction.
  * **margin_x** – The margin for the left and right sides of the element.
  * **margin_y** – The margin for the top and bottom sides of the element.
  * **position** – Specifies how the element is position.
  * **top** – The top position of the element.
  * **bottom** – The bottom position of the element.
  * **left** – The left position of the element.
  * **right** – The right position of the element.
  * **start** – The logical start position of the element, depending on layout direction.
  * **end** – The logical end position of the element, depending on layout direction.
  * **z_index** – The stacking order for the element
  * **is_hidden** – Hides the element.
  * **id** – The unique identifier of the element.
  * **UNSAFE_class_name** – Set the CSS className for the element. Only use as a last resort. Use style props instead.
  * **UNSAFE_style** – Set the inline style for the element. Only use as a last resort. Use style props instead.
