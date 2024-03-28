### deephaven.ui.table(table: Table, \*, on_row_press: Callable[[int, Dict[str, Any]], None] | None = None, on_row_double_press: Callable[[int, Dict[str, Any]], None] | None = None, on_cell_press: Callable[[Tuple[int, int], CellData], None] | None = None, on_cell_double_press: Callable[[Tuple[int, int], CellData], None] | None = None, on_column_press: Callable[[str], None] | None = None, on_column_double_press: Callable[[str], None] | None = None)

Customization to how a table is displayed, how it behaves, and listen to UI events.

Usage:

```python
from deephaven import empty_table, ui

t = empty_table(10).update("X=i")
my_table = ui.table(
    t, on_row_press=lambda row_index, row_data: print(row_index, row_data)
)
```

* **Parameters:**
  * **table** – The table to wrap
  * **on_row_press** – The callback function to run when a row is clicked.
    The first parameter is the row index, and the second is the row data provided in a dictionary where the
    column names are the keys.
  * **on_row_double_press** – The callback function to run when a row is double clicked.
    The first parameter is the row index, and the second is the row data provided in a dictionary where the
    column names are the keys.
  * **on_cell_press** – The callback function to run when a cell is clicked.
    The first parameter is the cell index, and the second is the row data provided in a dictionary where the
    column names are the keys.
  * **on_cell_double_press** – The callback function to run when a cell is double clicked.
    The first parameter is the cell index, and the second is the row data provided in a dictionary where the
    column names are the keys.
  * **on_column_press** – The callback function to run when a column is clicked.
    The first parameter is the column name.
  * **on_column_double_press** – The callback function to run when a column is double clicked.
    The first parameter is the column name.
* **Returns:**
  The table element.
