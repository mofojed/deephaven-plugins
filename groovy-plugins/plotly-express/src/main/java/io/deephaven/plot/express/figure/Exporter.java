package io.deephaven.plot.express.figure;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Assigns integer IDs to Deephaven table references that need to accompany a {@code NEW_FIGURE}
 * payload as {@code Object[]} alongside the JSON bytes. Mirrors the Python plugin's
 * {@code Exporter} (lives in {@code plot/express/exporter/export.py}).
 *
 * <p>The contract this plugin honors for now (static-figure milestone):
 * <ul>
 *   <li>Each unique table object gets a sequential int ID starting at 0.</li>
 *   <li>{@link #references()} returns the new objects to ship alongside the next payload, the
 *       new IDs assigned since the last call, and any removed IDs.</li>
 *   <li>This milestone only emits new references on the first {@code NEW_FIGURE} — no removal
 *       path because static figures don't lose tables.</li>
 * </ul>
 */
public final class Exporter {

    /** Identity-keyed so two Tables that compare {@code equals} aren't conflated. */
    private final IdentityHashMap<Object, Integer> tableIds = new IdentityHashMap<>();
    private final List<Object> pendingNewObjects = new ArrayList<>();
    private final List<Integer> pendingNewIds = new ArrayList<>();

    /** Reserve (or look up) an ID for a Deephaven Table. Returns the int written into the JSON mapping. */
    public int reference(Object table) {
        Integer existing = tableIds.get(table);
        if (existing != null) {
            return existing;
        }
        int id = tableIds.size();
        tableIds.put(table, id);
        pendingNewObjects.add(table);
        pendingNewIds.add(id);
        return id;
    }

    /**
     * Drain the pending new references and return them for inclusion in a NEW_FIGURE message.
     * After this call, the pending list is empty until further {@link #reference(Object)} calls.
     */
    public References references() {
        References out = new References(
                new ArrayList<>(pendingNewObjects),
                new ArrayList<>(pendingNewIds),
                new ArrayList<>());
        pendingNewObjects.clear();
        pendingNewIds.clear();
        return out;
    }

    /** Snapshot of refs to ship alongside a message payload. */
    public static final class References {
        public final List<Object> newObjects;
        public final List<Integer> newReferenceIds;
        public final List<Integer> removedReferenceIds;

        References(List<Object> newObjects, List<Integer> newReferenceIds, List<Integer> removedReferenceIds) {
            this.newObjects = newObjects;
            this.newReferenceIds = newReferenceIds;
            this.removedReferenceIds = removedReferenceIds;
        }
    }
}
