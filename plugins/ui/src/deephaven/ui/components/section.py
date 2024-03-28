from __future__ import annotations

from typing import Union, Any

from .._internal.utils import create_props
from ..elements import Element, BaseElement
from ..types import Stringable
from .item import ItemElement

PickerItem = Union[Stringable, ItemElement]
SectionElement = Element


def section(
    *children: PickerItem, title: str | None = None, **props: Any
) -> SectionElement:
    """
    A section that can be added to a menu, such as a `picker <picker>`_. Children are the dropdown options.

    Usage::

        my_picker = ui.picker(
            ui.section("Cat", "Dog", title="Domesticated"),
            ui.section("Lion", "Hyena", title="Wild"),
        )


    Args:
        *children: The options to render within the section.
        title: The title of the section.
        **props: Any other Section prop.

    Returns:
        The rendered Section.
    """

    children, props = create_props(locals())

    return BaseElement("deephaven.ui.components.Section", *children, **props)
