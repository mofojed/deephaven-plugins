### deephaven.ui.use_render_queue()

Returns a callback function for adding things to the render queue.
Should only be used if you’re loading data from a background thread and want to update state.

* **Returns:**
  A callback function that takes a function to call on the render thread.
