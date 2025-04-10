# This file will be prepended to the docs previews example file
#
# Use `ui_live_editor` to create an editor that you can enter code in and the result will appear below
# For demonstrative purposes.
#
# Example usage
# Creating a single editor:
# live_editor = ui_live_editor()
#
# Create a notebook, where you can add multiple editors:
# live_notebook = ui_live_notebook()
#
from deephaven import ui
from typing import Any, Callable, Dict, Union
import inspect


def _is_displayable(item: Any, filter_callables: bool = False):
    """
    Filters out modules and other non-displayable items
    """
    return (
        not filter_callables or not inspect.isfunction(item)
    ) and not inspect.ismodule(item)


@ui.component
def ui_live_item(item: Any, filter_callables: bool = False):
    """
    Display a UI for one object.
    If the object is a dict, displays it as tabs with a tab for each key.
    Otherwise just returns it.
    """
    if isinstance(item, Dict):
        filtered_items = list(
            filter(
                lambda sub_item: _is_displayable(sub_item[1], filter_callables),
                item.items(),
            )
        )
        if len(filtered_items) > 0:
            return ui.tabs(
                *(
                    ui.tab(ui_live_item(sub_item[1]), title=sub_item[0])
                    for sub_item in filtered_items
                ),
            )
        return ui.illustrated_message(
            ui.icon("book"),
            ui.heading("Empty dictionary"),
            ui.content("Dictionary does not contain any items."),
        )

    if isinstance(item, Callable):
        return ui.illustrated_message(
            ui.icon("code"),
            ui.heading("Callable"),
            ui.content(item.__class__.__name__),
        )

    return ui.view(
        ui.flex(item, direction="column", height="size-3000"),
        margin_bottom="size-100",
        min_height="size-3000",
        width="100%",
    )


@ui.component
def ui_live_result(code: str):
    """
    Runs the code passed in and displays the resulting components in tabs or an error view
    """

    def run_code() -> Union[Dict[str, Any], Exception]:
        try:
            # The exec documentation has some information that is pertinent here: https://docs.python.org/3/library/functions.html#exec
            # Use the same dictionary for `globals` and `locals`.
            # > When exec gets two separate objects as globals and locals, the code will be executed as if it were embedded in a class definition. This means functions and classes defined in the executed code will not be able to access variables assigned at the top level (as the "top level" variables are treated as class variables in a class definition).
            var_dict = {}
            exec(code, var_dict, var_dict)
            # Just clean out the `__builtins__` key that was automatically added. Everything else should have been added by our snippet.
            # > If the globals dictionary does not contain a value for the key __builtins__, a reference to the dictionary of the built-in module builtins is inserted under that key.
            del var_dict["__builtins__"]
            return var_dict

        except Exception as e:
            return e

    result = ui.use_memo(run_code, [code])

    if isinstance(result, Exception):
        return ui.illustrated_message(
            ui.icon("warning"), ui.heading("Error"), ui.content(str(result))
        )

    if len(result) == 0:
        return ui.illustrated_message(
            ui.icon("bug"),
            ui.heading("No variables found"),
            ui.content("Did you assign variables in your code?"),
        )

    return ui_live_item(result, True)
