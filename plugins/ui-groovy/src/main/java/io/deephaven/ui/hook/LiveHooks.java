package io.deephaven.ui.hook;

import io.deephaven.engine.rowset.RowSet;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.Table;
import io.deephaven.engine.table.TableUpdate;
import io.deephaven.engine.table.impl.InstrumentedTableUpdateListenerAdapter;
import io.deephaven.util.SafeCloseable;
import io.deephaven.util.locks.AwareFunctionalLock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Live-data hooks that subscribe to Deephaven {@link Table} updates and surface their data to
 * components. Mirrors Python's {@code use_table_data}, {@code use_table_listener},
 * {@code use_row_data}, {@code use_cell_data}, {@code use_column_data}.
 *
 * <p>Snapshot extraction is a List-of-Map representation (one map per row, column name → value).
 * For very large or wide tables, prefer {@link #useTableListener} and project the data yourself.
 *
 * <p>The hooks tolerate a {@code null} table (returns {@code null} for data, no-op for listener)
 * so callers don't need to special-case the unbound state.
 */
public final class LiveHooks {

    private LiveHooks() {}

    /**
     * Snapshot all rows of a table into a List of column-name-keyed Maps. Safe to call from the
     * render thread or an UpdateGraph notification.
     */
    public static List<Map<String, Object>> snapshot(Table table) {
        if (table == null) {
            return null;
        }
        Map<String, ? extends ColumnSource<?>> sources = table.getColumnSourceMap();
        RowSet rowSet = table.getRowSet();
        List<Map<String, Object>> rows = new ArrayList<>((int) Math.min(rowSet.size(), Integer.MAX_VALUE));
        rowSet.forAllRowKeys(rowKey -> {
            Map<String, Object> row = new LinkedHashMap<>(sources.size());
            for (Map.Entry<String, ? extends ColumnSource<?>> e : sources.entrySet()) {
                row.put(e.getKey(), e.getValue().get(rowKey));
            }
            rows.add(row);
        });
        return rows;
    }

    /**
     * Subscribe a listener to a refreshing table. The listener fires on the UpdateGraph thread on
     * every tick. If the table is null or static (non-refreshing) no listener is registered.
     *
     * @param listener {@code (update, isReplay)} consumer; {@code isReplay} is always {@code false}
     *                 in the current implementation.
     */
    public static void useTableListener(Table table,
                                        BiConsumer<TableUpdate, Boolean> listener,
                                        List<?> dependencies) {
        List<Object> deps = new ArrayList<>(dependencies == null ? List.of() : dependencies);
        deps.add(table);
        Hooks.useEffect(() -> {
            if (table == null || !table.isRefreshing()) {
                return null;
            }
            InstrumentedTableUpdateListenerAdapter adapter =
                    new InstrumentedTableUpdateListenerAdapter("deephaven.ui", table, false) {
                        @Override
                        public void onUpdate(TableUpdate update) {
                            listener.accept(update, Boolean.FALSE);
                        }
                    };
            // addUpdateListener requires the UpdateGraph's shared lock to be held — otherwise the
            // listener silently fails to wire (UpdateGraphConflictException with a
            // PoisonedUpdateGraph). Python's deephaven.table_listener.listen() does the same via
            // update_graph.auto_locking_ctx.
            AwareFunctionalLock lock = table.getUpdateGraph().sharedLock();
            try (SafeCloseable ignored = lock.lockCloseable()) {
                table.addUpdateListener(adapter);
            }
            return () -> {
                try (SafeCloseable ignored = lock.lockCloseable()) {
                    table.removeUpdateListener(adapter);
                }
            };
        }, deps);
    }

    /** Return the full table data; re-snapshots on every table tick. */
    public static List<Map<String, Object>> useTableData(Table table) {
        StateTuple<List<Map<String, Object>>> state = Hooks.useState(snapshot(table));
        useTableListener(table, (update, isReplay) -> state.setter().call(snapshot(table)), List.of());
        return state.value();
    }

    /** Return the first row, or {@code null} if the table is null/empty. */
    public static Map<String, Object> useRowData(Table table) {
        List<Map<String, Object>> data = useTableData(table);
        return (data == null || data.isEmpty()) ? null : data.get(0);
    }

    /** Return the top-left cell, or {@code null} if the table is null/empty. */
    public static Object useCellData(Table table) {
        Map<String, Object> row = useRowData(table);
        if (row == null || row.isEmpty()) {
            return null;
        }
        Iterator<Object> it = row.values().iterator();
        return it.hasNext() ? it.next() : null;
    }

    /** Return the values of the first column, in row order. */
    public static List<Object> useColumnData(Table table) {
        List<Map<String, Object>> data = useTableData(table);
        if (data == null) {
            return null;
        }
        if (data.isEmpty()) {
            return new ArrayList<>();
        }
        String firstColumn = data.get(0).keySet().iterator().next();
        List<Object> values = new ArrayList<>(data.size());
        for (Map<String, Object> row : data) {
            values.add(row.get(firstColumn));
        }
        return values;
    }
}
