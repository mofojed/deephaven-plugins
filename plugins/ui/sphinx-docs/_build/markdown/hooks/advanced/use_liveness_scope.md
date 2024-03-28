### deephaven.ui.use_liveness_scope(func: Callable, dependencies: Tuple[Any] | List[Any])

Wraps a Callable in a liveness scope, and ensures that when invoked, if that callable
causes the component to render, the scope will be retained by that render pass. It is
not appropriate to wrap functions that will be called within the render - this is intended
for calls made from outside a currently rendering component.

* **Parameters:**
  * **func** – The function to wrap
  * **dependencies** – Dependencies of the provided function, so the hook knows when to re-wrap it
* **Returns:**
  The wrapped Callable
