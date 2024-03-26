### deephaven.ui.use_state()

### deephaven.ui.use_state(initial_state: T | Callable[[], T])

Hook to add a state variable to your component. The state will persist across renders.

* **Parameters:**
  **initial_state** – The initial value for the state.
  It can be any type, but passing a function will treat it as an initializer function.
  An initializer function is called with no parameters once on the first render to get the initial value.
  After the initial render the argument is ignored.
  If an initial value is provided, only types matching that initial value will be valid when calling the returned set state function.
  If no initial value is provided, any type can be passed to the set state function.
* **Returns:**
  A tuple containing the current value of the state and a function to set the state.
  The set state function can take a new value or an updater function.
  - If the set state function is called with a new value, the state will be set to that value.
  - If the set state function is called with an updater function, the updater function will be called with the current state value and the new state will be set to the return value of the updater function.
