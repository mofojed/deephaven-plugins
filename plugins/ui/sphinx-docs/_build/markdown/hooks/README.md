# Hooks

Special functions prefixed with `use_` are called *hooks*. These functions must only be used at the *top* of a `@ui.component` or another hook. If you want to use one in a conditional or a loop, extract that logic to a new component and put it there.

## General hooks

General hooks for providing state, memoization, and other essential functionality for creating custom components.

* [`use_callback()`](general/use_callback.md)
* [`use_effect()`](general/use_effect.md)
* [`use_memo()`](general/use_memo.md)
* [`use_ref()`](general/use_ref.md)
* [`use_state()`](general/use_state.md)

## Table hooks

Use these hooks when you want to use data from a table on that python side:

* [`use_cell_data()`](table/use_cell_data.md)
* [`use_column_data()`](table/use_column_data.md)
* [`use_row_data()`](table/use_row_data.md)
* [`use_row_list()`](table/use_row_list.md)
* [`use_table_data()`](table/use_table_data.md)
* [`use_table_listener()`](table/use_table_listener.md)

## Advanced hooks

These hooks should only be used if you know what you’re doing.

* [`use_execution_context()`](advanced/use_execution_context.md)
* [`use_liveness_scope()`](advanced/use_liveness_scope.md)
* [`use_render_queue()`](advanced/use_render_queue.md)
