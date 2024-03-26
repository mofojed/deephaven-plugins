### deephaven.ui.use_memo(func: Callable[[], T], dependencies: Tuple[Any] | List[Any])

Memoize the result of a function call. The function will only be called again if the dependencies change.

* **Parameters:**
  * **func** – The function to memoize.
  * **dependencies** – The dependencies to check for changes.
* **Returns:**
  The memoized result of the function call.
