# Error Boundary

An error boundary catches rendering errors that occur in its child component tree, contains the error to the boundary, and displays a fallback UI instead of crashing the entire component. This allows the rest of a `deephaven.ui` widget to continue rendering when one part of it fails.

## Example

```python
from deephaven import ui


@ui.component
def ui_error_boundary():
    return ui.error_boundary(
        ui.text("This content is protected by an error boundary."),
    )


my_error_boundary = ui_error_boundary()
```

## Fallback

By default, the error boundary displays a built-in error message when it catches an error. Provide a `fallback` component to render custom content in its place instead. In this example, the child renders a `tabs` component with duplicate keys, which causes a rendering error that the boundary catches.

```python
from deephaven import ui


@ui.component
def ui_error_boundary_fallback():
    return ui.error_boundary(
        ui.tabs(
            ui.tab("Content A", title="A", key="duplicate"),
            ui.tab("Content B", title="B", key="duplicate"),
        ),
        fallback=ui.illustrated_message(
            ui.heading("Something went wrong"),
            ui.content("This section could not be rendered."),
        ),
    )


my_error_boundary_fallback = ui_error_boundary_fallback()
```

![Error Boundary Fallback Example](../_assets/error_boundary_fallback.png)

## Handling errors

The `on_error` prop accepts a callback that is invoked on the server with information about the error when one is caught. This is useful for logging errors or updating other parts of the UI in response.

```python
from deephaven import ui


@ui.component
def ui_error_boundary_on_error():
    error_message, set_error_message = ui.use_state(None)

    return ui.flex(
        ui.error_boundary(
            ui.tabs(
                ui.tab("Content A", title="A", key="duplicate"),
                ui.tab("Content B", title="B", key="duplicate"),
            ),
            fallback=ui.text("Unable to render tabs."),
            on_error=lambda error: set_error_message(error["message"]),
        ),
        ui.text(
            f"Caught error: {error_message}"
            if error_message is not None
            else "No errors caught."
        ),
        direction="column",
    )


my_error_boundary_on_error = ui_error_boundary_on_error()
```

## Containing errors

Errors are contained to the boundary that catches them. Components rendered outside of the boundary continue to render normally, even when the content inside the boundary fails.

```python
from deephaven import ui


@ui.component
def ui_error_boundary_contained():
    return ui.flex(
        ui.text("This text renders normally outside the boundary."),
        ui.error_boundary(
            ui.tabs(
                ui.tab("Content A", title="A", key="duplicate"),
                ui.tab("Content B", title="B", key="duplicate"),
            ),
            fallback=ui.text("This boundary caught an error."),
        ),
        direction="column",
    )


my_error_boundary_contained = ui_error_boundary_contained()
```

## UI recommendations

1. Wrap sections of a widget that may fail independently in their own `error_boundary` so a failure in one section does not take down the entire widget.
2. Provide a `fallback` that gives the user meaningful context about what failed rather than relying on the default error message.
3. Use `on_error` to log errors or report them to the rest of your application, but avoid putting expensive or error-prone work in the callback itself.
4. Error boundaries only catch errors that occur while rendering on the client. They do not catch errors raised while a `@ui.component` function executes on the server.

## API Reference

```{eval-rst}
.. dhautofunction:: deephaven.ui.error_boundary
```
