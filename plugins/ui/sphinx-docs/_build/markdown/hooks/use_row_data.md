### deephaven.ui.use_row_data(table: Table, sentinel: Any | None = None)

Return the first row of the table as a dictionary. The table should already be filtered to only have a single row.

* **Parameters:**
  * **table** – The table to extract the row from.
  * **sentinel** – The sentinel value to return if the table is ticking but empty. Defaults to None.
* **Returns:**
  The first row of the table as a dictionary or the sentinel value.
