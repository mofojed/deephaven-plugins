### deephaven.ui.use_table_data(table: Table, sentinel: Any | None = None, transform: Callable[[DataFrame | Any, bool], Any] | None = None)

Returns a dictionary with the contents of the table. Component will redraw if the table
changes, resulting in an updated frame.

* **Parameters:**
  * **table** – The table to listen to.
  * **sentinel** – The sentinel value to return if the table is ticking but empty. Defaults to None.
  * **transform** – A function to transform the table data and is_sentinel values. Defaults to None, which will
    return the data as TableData.
* **Returns:**
  The table data or the sentinel value.
