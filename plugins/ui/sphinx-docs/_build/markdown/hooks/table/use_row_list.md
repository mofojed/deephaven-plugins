### deephaven.ui.use_row_list(table: Table, sentinel: Any | None = None)

Return the first row of the table as a list. The table should already be filtered to only have a single row.

* **Parameters:**
  * **table** – The table to extract the row from.
  * **sentinel** – The sentinel value to return if the table is ticking but empty. Defaults to None.
* **Returns:**
  The first row of the table as a list or the sentinel value.
