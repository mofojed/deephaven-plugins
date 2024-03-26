### deephaven.ui.use_table_listener(table: Table, listener: Callable[[TableUpdate, bool], None] | TableListener, dependencies: Tuple[Any] | List[Any], description: str | None = None, do_replay: bool = False, replay_lock: Literal['shared', 'exclusive'] = 'shared')

Listen to a table and call a listener when the table updates.

* **Parameters:**
  * **table** – The table to listen to.
  * **listener** – Either a function or a TableListener with an on_update function.
    The function must take a TableUpdate and is_replay bool.
  * **dependencies** – Dependencies of the listener function, so the hook knows when to recreate the listener
  * **description** – An optional description for the UpdatePerformanceTracker to append to the listener’s
    entry description, default is None.
  * **do_replay** – Whether to replay the initial snapshot of the table, default is False.
  * **replay_lock** – The lock type used during replay, default is ‘shared’, can also be ‘exclusive’.
