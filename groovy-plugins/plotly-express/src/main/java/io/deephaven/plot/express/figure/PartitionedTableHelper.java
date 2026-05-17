package io.deephaven.plot.express.figure;

import java.util.ArrayList;
import java.util.List;

/**
 * Reflective wrappers around {@code io.deephaven.engine.table.PartitionedTable} so the figure
 * builders can detect + iterate partitions without forcing the engine JARs onto the unit-test
 * classpath. The server classpath always has them.
 *
 * <p>Mirrors what Python's {@code PartitionManager} does when handed a {@code PartitionedTable}.
 */
public final class PartitionedTableHelper {

    private PartitionedTableHelper() {}

    /** True if {@code obj} is a {@code io.deephaven.engine.table.PartitionedTable}. */
    public static boolean isPartitioned(Object obj) {
        if (obj == null) {
            return false;
        }
        for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> iface : c.getInterfaces()) {
                if ("io.deephaven.engine.table.PartitionedTable".equals(iface.getName())) {
                    return true;
                }
            }
            if ("io.deephaven.engine.table.PartitionedTable".equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    /** Snapshot of each partition: the constituent Table + the partition key value as a string. */
    public static final class Partition {
        public final Object table;
        public final String keyLabel;
        Partition(Object table, String keyLabel) {
            this.table = table;
            this.keyLabel = keyLabel;
        }
    }

    /**
     * Snapshot the partitions in declaration order. Uses the meta table's
     * {@code columnIterator(String)} (returns a CloseableIterator) for the key column and the
     * constituent column. Errors propagate as RuntimeExceptions so the caller can see what went
     * wrong instead of silently rendering an empty figure.
     */
    public static List<Partition> snapshot(Object partitionedTable) {
        try {
            Object metaTable = partitionedTable.getClass()
                    .getMethod("table").invoke(partitionedTable);
            String constituentColumn = (String) partitionedTable.getClass()
                    .getMethod("constituentColumnName").invoke(partitionedTable);
            Object keyColNames = partitionedTable.getClass()
                    .getMethod("keyColumnNames").invoke(partitionedTable);
            @SuppressWarnings("unchecked")
            List<String> keyCols = new ArrayList<>((java.util.Collection<String>) keyColNames);
            if (keyCols.isEmpty()) {
                return new ArrayList<>();
            }

            List<Object> keyValues = columnValues(metaTable, keyCols.get(0));
            List<Object> tables = columnValues(metaTable, constituentColumn);
            int n = Math.min(keyValues.size(), tables.size());
            List<Partition> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                out.add(new Partition(tables.get(i), String.valueOf(keyValues.get(i))));
            }
            return out;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("PartitionedTableHelper.snapshot failed: " + e.getMessage(), e);
        }
    }

    /**
     * Drain {@code Table.columnIterator(column)} into a list. The returned object is a
     * {@code CloseableIterator}; we close it at the end. Uses reflection because
     * engine-table isn't on the unit-test classpath.
     */
    private static List<Object> columnValues(Object table, String column) throws ReflectiveOperationException {
        Object iter = table.getClass().getMethod("columnIterator", String.class).invoke(table, column);
        try {
            List<Object> out = new ArrayList<>();
            while ((boolean) iter.getClass().getMethod("hasNext").invoke(iter)) {
                out.add(iter.getClass().getMethod("next").invoke(iter));
            }
            return out;
        } finally {
            try {
                iter.getClass().getMethod("close").invoke(iter);
            } catch (ReflectiveOperationException ignored) {
                // Some iterator impls don't expose close — fine, GC handles them.
            }
        }
    }
}
