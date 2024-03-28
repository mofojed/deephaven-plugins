# Components

deephaven.ui provides many components to display in the UI. Use a component by assigning it to a variable, then display it in the UI.

```python
my_button = ui.button("Click Me!", on_press=lambda e: print(f"Button was clicked! {e}"))
```

Select any of the components below for API reference details. For examples and use cases, see the [examples](../examples) page.

## Buttons

Button components are used to trigger actions via user interaction:

```{toctree}
:glob:
:maxdepth: 1
buttons/*
```

## Inputs

Get input from the user with input components:

```{toctree}
:glob:
:maxdepth: 1
inputs/*
```

## Content

Display content in the UI with content components:

```{toctree}
:glob:
:maxdepth: 1
content/*
```

## Layout

Organize components in the UI with layout components:

```{toctree}
:glob:
:maxdepth: 1
layout/*
```
