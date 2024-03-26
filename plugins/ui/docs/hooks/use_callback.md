### deephaven.ui.use_callback(func: Callable, dependencies: Tuple[Any] | List[Any])

Create a stable handle for a callback function. The callback will only be recreated if the dependencies change.

* **Parameters:**
  * **func** – The function to create a stable handle to.
  * **dependencies** – The dependencies to check for changes.
* **Returns:**
  The stable handle to the callback function.
