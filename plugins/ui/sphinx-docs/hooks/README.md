# Hooks

Special functions prefixed with `use_` are called _hooks_. These functions must only be used at the _top_ of a `@ui.component` or another hook. If you want to use one in a conditional or a loop, extract that logic to a new component and put it there.

## General hooks

- [use_callback](use_callback)
- [use_effect](use_effect)
- [use_memo](use_memo)
- [use_ref](use_ref)
- [use_state](use_state)

## Table hooks

- [use_cell_data](use_cell_data)
- [use_column_data](use_column_data)
- [use_row_data](use_row_data)
- [use_row_list](use_row_list)
- [use_table_data](use_table_data)
- [use_table_listener](use_table_listener)

## Advanced hooks

These hooks should only be used if you know what you're doing.

- [use_execution_context](use_execution_context)
- [use_liveness_scope](use_liveness_scope)
- [use_render_queue](use_render_queue)
