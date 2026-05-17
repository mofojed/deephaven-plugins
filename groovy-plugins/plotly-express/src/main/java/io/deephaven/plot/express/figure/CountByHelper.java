package io.deephaven.plot.express.figure;

/**
 * Reflective wrapper around {@code Table.view(String...).countBy("count", byCol)} so the figure
 * builders can produce a server-side count aggregation without forcing the engine JARs onto the
 * unit-test classpath. The server classpath always has them.
 *
 * <p>Mirrors what Python's {@code FreqPreprocessor.preprocess_partitioned_tables} does in
 * {@code plot/express/preprocess/FreqPreprocessor.py}.
 */
public final class CountByHelper {

    private CountByHelper() {}

    /**
     * Returns a new Table with one row per distinct {@code byColumn} value and a {@code count}
     * column. Equivalent to {@code table.view([byColumn]).countBy("count", byColumn)}.
     *
     * @return the new Table (server classpath only; null if reflection fails — in which case
     *         the caller should fall back to passing the original table).
     */
    public static Object countBy(Object table, String byColumn) {
        try {
            Object viewed = table.getClass()
                    .getMethod("view", String[].class)
                    .invoke(table, (Object) new String[]{byColumn});
            return viewed.getClass()
                    .getMethod("countBy", String.class, String[].class)
                    .invoke(viewed, "count", new String[]{byColumn});
        } catch (ReflectiveOperationException e) {
            // engine-table not on classpath (unit tests) — fall back to the original table.
            return table;
        }
    }
}
