from typing import Literal, Optional, Union
from ..elements import SpectrumElement
from .._internal import dict_to_camel_case


def flex(
    *children,
    direction: Literal["row", "column", "row-reverse", "column-reverse"] = None,
    wrap: Literal["wrap", "nowrap", "wrap-reverse"] = None,
    justify_content: Literal[
        "start",
        "end",
        "center",
        "left",
        "right",
        "space-between",
        "space-around",
        "space-evenly",
        "stretch",
        "baseline",
        "first baseline",
        "last baseline",
        "safe center",
        "unsafe center",
    ] = None,
    align_content: Literal[
        "start",
        "end",
        "center",
        "space-between",
        "space-around",
        "space-evenly",
        "stretch",
        "baseline",
        "first baseline",
        "last baseline",
        "safe center",
        "unsafe center",
    ] = None,
    align_items: Optional[
        Literal[
            "start",
            "end",
            "center",
            "stretch",
            "self-start",
            "self-end",
            "baseline" "first baseline",
            "last baseline",
            "safe center",
            "unsafe center",
        ]
    ] = None,
    gap: Optional[Union[str, int, float]] = None,
    column_gap: Optional[Union[str, int, float]] = None,
    row_gap: Optional[Union[str, int, float]] = None,
    **props
):
    """
    Python implementation for the Adobe React Spectrum Flex component.
    https://react-spectrum.adobe.com/react-spectrum/Flex.html
    """
    return SpectrumElement(
        "Flex",
        *children,
        direction=direction,
        wrap=wrap,
        justify_content=justify_content,
        align_content=align_content,
        align_items=align_items,
        gap=gap,
        column_gap=column_gap,
        row_gap=row_gap,
        **props
    )
