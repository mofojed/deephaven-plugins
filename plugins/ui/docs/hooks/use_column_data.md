### deephaven.ui.use_column_data(table: Table, sentinel: Any | None = None)

Return the first column of the table as a list. The table should already be filtered to only have a single column.

* **Parameters:**
  * **table** – The table to extract the column from.
  * **sentinel** – The sentinel value to return if the table is ticking but empty. Defaults to None.
* **Returns:**
  The first column of the table as a list or the sentinel value.
