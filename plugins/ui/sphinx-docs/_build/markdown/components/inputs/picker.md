### deephaven.ui.picker(\*children: str | int | float | bool | BaseElement | Element | Table | PartitionedTable, key_column: str | None = None, label_column: str | None = None, description_column: str | None = None, icon_column: str | None = None, title_column: str | None = None, default_selected_key: str | int | float | bool | None = None, selected_key: str | int | float | bool | None = None, on_selection_change: Callable[[str | int | float | bool], None] | None = None, on_change: Callable[[str | int | float | bool], None] | None = None, \*\*props: Any)

Based on Spectrum’s [Picker](https://react-spectrum.adobe.com/react-spectrum/Picker.html) component.
A picker that can be used to select from a list. Children should be one of four types:
If children are of type PickerItem, they are the dropdown options.
If children are of type SectionElement, they are the dropdown sections.
If children are of type Table, the values in the table are the dropdown options. There can only be one child, the Table.
If children are of type PartitionedTable, the values in the table are the dropdown options and the partitions create multiple sections. There can only be one child, the PartitionedTable.

Usage:

```python
my_picker = ui.picker(
    "Option 1",
    "Option 2",
    selected_key="Option 2",
    on_change=lambda new_key: print(f"Selection changed {new_key}!"),
)
```

* **Parameters:**
  * **\*children** – The options to render within the picker.
  * **key_column** – Only valid if children are of type Table or PartitionedTable.
    The column of values to use as item keys. Defaults to the first column.
  * **label_column** – Only valid if children are of type Table or PartitionedTable.
    The column of values to display as primary text. Defaults to the key_column value.
  * **description_column** – Only valid if children are of type Table or PartitionedTable.
    The column of values to display as descriptions.
  * **icon_column** – Only valid if children are of type Table or PartitionedTable.
    The column of values to map to icons.
  * **title_column** – Only valid if children is of type PartitionedTable.
    The column of values to display as section names.
    Should be the same for all values in the constituent Table.
    If not specified, the section titles will be created from the key_columns of the PartitionedTable.
  * **default_selected_key** – The initial selected key in the collection (uncontrolled).
  * **selected_key** – The currently selected key in the collection (controlled).
  * **on_selection_change** – Handler that is called when the selection changes.
  * **on_change** – Alias of on_selection_change. Handler that is called when the selection changes.
  * **\*\*props** – Any other Picker prop, except items.
* **Returns:**
  A picker element.
