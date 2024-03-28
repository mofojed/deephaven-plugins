### deephaven.ui.use_execution_context(exec_ctx: ExecutionContext | None = None)

Create an execution context wrapper for a function.

* **Parameters:**
  **exec_ctx** – ExecutionContext: The execution context to use. Defaults to
  the current execution context if not provided.
* **Returns:**
  A callable that will take any callable and invoke it within the current exec context
