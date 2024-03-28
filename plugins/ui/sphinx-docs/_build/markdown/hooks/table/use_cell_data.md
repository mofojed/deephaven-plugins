### deephaven.ui.use_cell_data(table: Table, sentinel: Any | None = None)

Return the first cell of the table. The table should already be filtered to only have a single cell.

* **Parameters:**
  * **table** – The table to extract the cell from.
  * **sentinel** – The sentinel value to return if the table is ticking but empty. Defaults to None.
* **Returns:**
  The first cell of the table.
* **Return type:**
  Any
