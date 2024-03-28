### deephaven.ui.use_effect(func: Callable[[], Any], dependencies: Tuple[Any] | List[Any])

Call a function when the dependencies change. Optionally return a cleanup function to be called when dependencies change again or component is unmounted.

* **Parameters:**
  * **func** – The function to call when the dependencies change.
  * **dependencies** – The dependencies to check for changes.
* **Returns:**
  None
