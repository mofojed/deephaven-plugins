# Hooks

Special functions prefixed with `use_` are called _hooks_. These functions must only be used at the _top_ of a `@ui.component` or another hook. If you want to use one in a conditional or a loop, extract that logic to a new component and put it there.

## General hooks

- [use_callback](use_callback.md)
- [use_effect](use_effect.md)
- [use_memo](use_memo.md)
- [use_ref](use_ref.md)
- [use_state](use_state.md)

## Table hooks

- [use_cell_data](use_cell_data.md)
- [use_column_data](use_column_data.md)
- [use_row_data](use_row_data.md)
- [use_row_list](use_row_list.md)
- [use_table_data](use_table_data.md)
- [use_table_listener](use_table_listener.md)

## Advanced hooks

These hooks should only be used if you know what you're doing.

- [use_execution_context](use_execution_context.md)
- [use_liveness_scope](use_liveness_scope.md)
- [use_render_queue](use_render_queue.md)
