package io.deephaven.plot.express.figure;

/**
 * Resolves a Deephaven table column's runtime type to one of the {@link Placeholder.Kind} buckets
 * used for placeholder data emission. Kept as a single static entry so unit tests can stub a
 * table without dragging the engine into the test classpath.
 *
 * <p>Reflective lookups are used so the unit-test classpath does not need the engine JARs.
 * The server classpath always has them.
 */
public final class ColumnTypeResolver {

    private ColumnTypeResolver() {}

    /**
     * @param table  a Deephaven {@code io.deephaven.engine.table.Table} (or anything exposing the
     *               same {@code getDefinition().getColumn(name).getDataType()} chain).
     * @param column the column name to inspect.
     * @return a {@link Placeholder.Kind} describing how to encode the placeholder value.
     */
    public static Placeholder.Kind kindOf(Object table, String column) {
        Class<?> dataType = resolveDataType(table, column);
        if (dataType == null) {
            return Placeholder.Kind.STRING;
        }
        if (dataType == int.class || dataType == Integer.class
                || dataType == short.class || dataType == Short.class
                || dataType == byte.class || dataType == Byte.class) {
            return Placeholder.Kind.INT;
        }
        if (dataType == long.class || dataType == Long.class) {
            return Placeholder.Kind.LONG;
        }
        if (dataType == double.class || dataType == Double.class
                || dataType == float.class || dataType == Float.class) {
            return Placeholder.Kind.DOUBLE;
        }
        // String, char, java.time.Instant, etc. all fall back to string-shaped placeholders;
        // the JS client overwrites them with real data through the mapping.
        return Placeholder.Kind.STRING;
    }

    private static Class<?> resolveDataType(Object table, String column) {
        try {
            Object def = table.getClass().getMethod("getDefinition").invoke(table);
            Object col = def.getClass().getMethod("getColumn", String.class).invoke(def, column);
            return (Class<?>) col.getClass().getMethod("getDataType").invoke(col);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
