from __future__ import annotations
from typing import Any, Callable
from .types import (
    FocusEventCallable,
    KeyboardEventCallable,
    AlignSelf,
    CSSProperties,
    DimensionValue,
    JustifySelf,
    LayoutFlex,
    Position,
)
from .basic import component_element
from ..elements import Element


def switch(
    *children: Any,
    is_emphasized: bool | None = None,
    default_selected: bool | None = None,
    is_selected: bool | None = None,
    value: str | None = None,
    is_disabled: bool | None = None,
    is_read_only: bool | None = None,
    auto_focus: bool | None = None,
    is_quiet: bool | None = None,
    name: str | None = None,
    on_change: Callable[[bool], None] | None = None,
    on_focus: FocusEventCallable | None = None,
    on_blur: FocusEventCallable | None = None,
    on_focus_change: Callable[[bool], None] | None = None,
    on_key_down: KeyboardEventCallable | None = None,
    on_key_up: KeyboardEventCallable | None = None,
    flex: LayoutFlex | None = None,
    flex_grow: float | None = None,
    flex_shrink: float | None = None,
    flex_basis: DimensionValue | None = None,
    align_self: AlignSelf | None = None,
    justify_self: JustifySelf | None = None,
    order: int | None = None,
    grid_area: str | None = None,
    grid_column: str | None = None,
    grid_row: str | None = None,
    grid_column_start: str | None = None,
    grid_column_end: str | None = None,
    grid_row_start: str | None = None,
    grid_row_end: str | None = None,
    margin: DimensionValue | None = None,
    margin_top: DimensionValue | None = None,
    margin_bottom: DimensionValue | None = None,
    margin_start: DimensionValue | None = None,
    margin_end: DimensionValue | None = None,
    margin_x: DimensionValue | None = None,
    margin_y: DimensionValue | None = None,
    width: DimensionValue | None = None,
    height: DimensionValue | None = None,
    min_width: DimensionValue | None = None,
    min_height: DimensionValue | None = None,
    max_width: DimensionValue | None = None,
    max_height: DimensionValue | None = None,
    position: Position | None = None,
    top: DimensionValue | None = None,
    bottom: DimensionValue | None = None,
    left: DimensionValue | None = None,
    right: DimensionValue | None = None,
    start: DimensionValue | None = None,
    end: DimensionValue | None = None,
    z_index: int | None = None,
    is_hidden: bool | None = None,
    id: str | None = None,
    exclude_from_tab_order: bool | None = None,
    aria_controls: str | None = None,
    aria_label: str | None = None,
    aria_labelledby: str | None = None,
    aria_describedby: str | None = None,
    aria_details: str | None = None,
    UNSAFE_class_name: str | None = None,
    UNSAFE_style: CSSProperties | None = None,
) -> Element:
    """
    Switches allow users to turn an individual option on or off. They are usually used to activate or deactivate a specific setting.

    Args:
        *children: The children to render inside the input.
        is_emphasized: Whether the input should be displayed with an emphasized style.
        default_selected: Whether the input is selected by default.
        is_selected: Whether the input is selected.
        value: THe value of the input element, used when submitting a form.
        is_disabled: Whether the input is disabled.
        is_read_only: Whether the input can be selected but not changed by the user.
        auto_focus: Whether the input should automatically get focus when the page loads.
        is_quiet: Whether the input should be quiet.
        name: The name of the input, used when submitting a form.
        on_change: Handler that is called when the element's selection state changes.
        on_focus: Function called when the input receives focus.
        on_blur: Function called when the input loses focus.
        on_focus_change: Function called when the focus state changes.
        on_key_down: Function called when a key is pressed.
        on_key_up: Function called when a key is released.
        flex: When used in a flex layout, specifies how the element will grow or shrink to fit the space available.
        flex_grow: When used in a flex layout, specifies how much the element will grow to fit the space available.
        flex_shrink: When used in a flex layout, specifies how much the element will shrink to fit the space available.
        flex_basis: When used in a flex layout, specifies the initial size of the element.
        align_self: Overrides the align_items property of a flex or grid container.
        justify_self: Specifies how the element is justified inside a flex or grid container.
        order: The layout order for the element within a flex or grid container.
        grid_area: The name of the grid area to place the element in.
        grid_row: The name of the grid row to place the element in.
        grid_row_start: The name of the grid row to start the element in.
        grid_row_end: The name of the grid row to end the element in.
        grid_column: The name of the grid column to place the element in.
        grid_column_start: The name of the grid column to start the element in.
        grid_column_end: The name of the grid column to end the element in.
        margin: The margin to apply around the element.
        margin_top: The margin to apply above the element.
        margin_bottom: The margin to apply below the element.
        margin_start: The margin to apply before the element.
        margin_end: The margin to apply after the element.
        margin_x: The margin to apply to the left and right of the element.
        margin_y: The margin to apply to the top and bottom of the element.
        width: The width of the element.
        height: The height of the element.
        min_width: The minimum width of the element.
        min_height: The minimum height of the element.
        max_width: The maximum width of the element.
        max_height: The maximum height of the element.
        position: Specifies how the element is positioned.
        top: The distance from the top of the containing element.
        bottom: The distance from the bottom of the containing element.
        start: The distance from the start of the containing element.
        end: The distance from the end of the containing element.
        left: The distance from the left of the containing element.
        right: The distance from the right of the containing element.
        z_index: The stack order of the element.
        is_hidden: Whether the element is hidden.
        id: A unique identifier for the element.
        exclude_from_tab_order: Whether the element should be excluded from the tab order.
        aria_controls: The id of the element that the element controls.
        aria_label: The label for the element.
        aria_labelledby: The id of the element that labels the element.
        aria_describedby: The id of the element that describes the element.
        aria_details: The details for the element.
        UNSAFE_class_name: A CSS class to apply to the element.
        UNSAFE_style: A CSS style to apply to the element.
    """

    return component_element(
        "Switch",
        *children,
        is_emphasized=is_emphasized,
        is_selected=is_selected,
        value=value,
        default_selected=default_selected,
        is_disabled=is_disabled,
        is_read_only=is_read_only,
        auto_focus=auto_focus,
        is_quiet=is_quiet,
        name=name,
        on_change=on_change,
        on_focus=on_focus,
        on_blur=on_blur,
        on_focus_change=on_focus_change,
        on_key_down=on_key_down,
        on_key_up=on_key_up,
        flex=flex,
        flex_grow=flex_grow,
        flex_shrink=flex_shrink,
        flex_basis=flex_basis,
        align_self=align_self,
        justify_self=justify_self,
        order=order,
        grid_area=grid_area,
        grid_column=grid_column,
        grid_row=grid_row,
        grid_column_start=grid_column_start,
        grid_column_end=grid_column_end,
        grid_row_start=grid_row_start,
        grid_row_end=grid_row_end,
        margin=margin,
        margin_top=margin_top,
        margin_bottom=margin_bottom,
        margin_start=margin_start,
        margin_end=margin_end,
        margin_x=margin_x,
        margin_y=margin_y,
        width=width,
        height=height,
        min_width=min_width,
        min_height=min_height,
        max_width=max_width,
        max_height=max_height,
        position=position,
        top=top,
        bottom=bottom,
        left=left,
        right=right,
        start=start,
        end=end,
        z_index=z_index,
        is_hidden=is_hidden,
        id=id,
        exclude_from_tab_order=exclude_from_tab_order,
        aria_controls=aria_controls,
        aria_label=aria_label,
        aria_labelledby=aria_labelledby,
        aria_describedby=aria_describedby,
        aria_details=aria_details,
        UNSAFE_class_name=UNSAFE_class_name,
        UNSAFE_style=UNSAFE_style,
    )
