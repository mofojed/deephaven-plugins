# deephaven.ui documentation

deephaven.ui is a plugin for Deephaven that allows for programmatic layouts and callbacks. It uses a React-like approach to building components and rendering them in the UI, allowing for creating reactive components that can be re-used and composed together, as well as reacting to user input from the UI.

## Getting Started

First, follow the guide to [Start a Deephaven Server](https://deephaven.io/core/docs/tutorials/pip-install/). Then, install the deephaven.ui plugin using `pip install`:

```sh
pip install deephaven-ui
```

You’ll then need to restart your Deephaven server to load the plugin.

Once you have the Deephaven up, you can assign deephaven.ui components to variables and display them in the UI. For example, to display a simple button:

```python
from deephaven import ui

my_button = ui.button("Click Me!", on_press=lambda e: print(f"Button was clicked! {e}"))
```

## Contents

* [Quick start](tutorials/quick-start.md)
  * [Handling events](tutorials/quick-start.md#handling-events)
  * [Creating components](tutorials/quick-start.md#creating-components)
  * [Using state](tutorials/quick-start.md#using-state)
  * [Sharing state](tutorials/quick-start.md#sharing-state)
* [Components](components/README.md)
  * [Buttons](components/README.md#buttons)
    * [`action_button()`](components/buttons/action_button.md)
    * [`button()`](components/buttons/button.md)
    * [`button_group()`](components/buttons/button_group.md)
    * [`toggle_button()`](components/buttons/toggle_button.md)
  * [Inputs](components/README.md#inputs)
    * [`checkbox()`](components/inputs/checkbox.md)
    * [`form()`](components/inputs/form.md)
    * [`number_field()`](components/inputs/number_field.md)
    * [`picker()`](components/inputs/picker.md)
    * [`range_slider()`](components/inputs/range_slider.md)
    * [`slider()`](components/inputs/slider.md)
    * [`switch()`](components/inputs/switch.md)
    * [`text_field()`](components/inputs/text_field.md)
  * [Content](components/README.md#content)
    * [`content()`](components/content/content.md)
    * [`contextual_help()`](components/content/contextual_help.md)
    * [`flex()`](components/content/flex.md)
    * [`flex()`](components/content/fragment.md)
    * [`grid()`](components/content/grid.md)
    * [`heading()`](components/content/heading.md)
    * [`icon()`](components/content/icon.md)
    * [`illustrated_message()`](components/content/illustrated_message.md)
    * [`item()`](components/content/item.md)
    * [`table()`](components/content/table.md)
    * [`tabs()`](components/content/tabs.md)
    * [`tab_panels()`](components/content/tabs.md#deephaven.ui.tab_panels)
    * [`tab_list()`](components/content/tabs.md#deephaven.ui.tab_list)
    * [`view()`](components/content/view.md)
  * [Layout](components/README.md#layout)
    * [`column()`](components/layout/column.md)
    * [`dashboard()`](components/layout/dashboard.md)
    * [`panel()`](components/layout/panel.md)
    * [`row()`](components/layout/row.md)
    * [`stack()`](components/layout/stack.md)
* [Hooks](hooks/README.md)
  * [General hooks](hooks/README.md#general-hooks)
    * [`use_callback()`](hooks/general/use_callback.md)
    * [`use_effect()`](hooks/general/use_effect.md)
    * [`use_memo()`](hooks/general/use_memo.md)
    * [`use_ref()`](hooks/general/use_ref.md)
    * [`use_state()`](hooks/general/use_state.md)
  * [Table hooks](hooks/README.md#table-hooks)
    * [`use_cell_data()`](hooks/table/use_cell_data.md)
    * [`use_column_data()`](hooks/table/use_column_data.md)
    * [`use_row_data()`](hooks/table/use_row_data.md)
    * [`use_row_list()`](hooks/table/use_row_list.md)
    * [`use_table_data()`](hooks/table/use_table_data.md)
    * [`use_table_listener()`](hooks/table/use_table_listener.md)
  * [Advanced hooks](hooks/README.md#advanced-hooks)
    * [`use_execution_context()`](hooks/advanced/use_execution_context.md)
    * [`use_liveness_scope()`](hooks/advanced/use_liveness_scope.md)
    * [`use_render_queue()`](hooks/advanced/use_render_queue.md)
* [Architecture](architecture.md)
  * [Rendering](architecture.md#rendering)
  * [Communication/Callbacks](architecture.md#communication-callbacks)
  * [Communication Layers](architecture.md#communication-layers)
