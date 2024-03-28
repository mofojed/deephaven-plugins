# deephaven.ui documentation

deephaven.ui is a plugin for Deephaven that allows for programmatic layouts and callbacks. It uses a React-like approach to building components and rendering them in the UI, allowing for creating reactive components that can be re-used and composed together, as well as reacting to user input from the UI.

## Getting Started

First, follow the guide to [Start a Deephaven Server](https://deephaven.io/core/docs/tutorials/pip-install/). Then, install the deephaven.ui plugin using `pip install`:

```sh
pip install deephaven-ui
```

You'll then need to restart your Deephaven server to load the plugin.

Once you have the Deephaven up, you can assign deephaven.ui components to variables and display them in the UI. For example, to display a simple button:

```python
from deephaven import ui

my_button = ui.button("Click Me!", on_press=lambda e: print(f"Button was clicked! {e}"))
```

## Contents

```{toctree}
:maxdepth: 3
:glob:
tutorials/*
components/README
hooks/README
architecture
```
