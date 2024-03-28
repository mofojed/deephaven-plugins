from ..elements import BaseElement


def icon(name: str, *children, **props):
    """
    Based on Spectrum's `Icon <https://react-spectrum.adobe.com/react-spectrum/Icon.html>`_ component.
    Get an icon by name.

    Usage::

        my_icon = ui.icon("dhTruck")

    Args:
        name: The name of the icon to render.

    Returns:
        The icon element.
    """
    return BaseElement(f"deephaven.ui.icons.{name}", *children, **props)
