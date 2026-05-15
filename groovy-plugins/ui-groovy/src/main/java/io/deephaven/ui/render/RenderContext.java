package io.deephaven.ui.render;

import io.deephaven.engine.liveness.LivenessScope;
import io.deephaven.engine.liveness.LivenessScopeStack;
import io.deephaven.util.SafeCloseable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * React-style per-component render state. Direct port of Python's {@code RenderContext}: hook
 * slots, child contexts, effects, unmount listeners, plus liveness-scope plumbing for derived
 * live objects produced inside the render. The current context is exposed via a {@link ThreadLocal}
 * so {@link io.deephaven.ui.hook.Hooks} can find it implicitly.
 *
 * <p>Each render opens a fresh top-level {@link LivenessScope} which captures any
 * {@code LivenessReferent}s allocated during render (e.g., derived tables built directly in the
 * component body). Hooks that own their own scope ({@code useMemo}, {@code useLivenessScope})
 * register it via {@link #manage(LivenessScope)} so the context retains it across renders. After
 * a successful render, scopes that were owned by the previous render but are no longer in
 * {@link #collectedScopes} are released — same lifecycle the Python plugin uses.
 */
public final class RenderContext {

    private static final ThreadLocal<RenderContext> CURRENT = new ThreadLocal<>();

    private static final int READY_TO_OPEN = -2;
    private static final int OPENED_AND_UNUSED = -1;

    private final RootRenderContext root;
    private final Map<Integer, Object> state = new LinkedHashMap<>();
    private final Map<String, RenderContext> childrenContext = new LinkedHashMap<>();

    private int hookIndex = READY_TO_OPEN;
    private int hookCount = -1;
    private boolean isMounted = true;

    private final List<Effect> collectedEffects = new ArrayList<>();
    private final List<Runnable> collectedUnmountListeners = new ArrayList<>();
    private final List<String> collectedContexts = new ArrayList<>();
    private final List<Runnable> openCleanups = new ArrayList<>();

    /** Scopes currently owned by this context (released when no longer referenced post-render). */
    private Set<LivenessScope> collectedScopes = new HashSet<>();
    /** Top-level scope opened while this context is active — captures objects created in render. */
    private LivenessScope topLevelScope;

    public RenderContext(RootRenderContext root) {
        this.root = root;
    }

    /** Returns the currently active context; throws {@link NoContextException} if none. */
    public static RenderContext current() {
        RenderContext c = CURRENT.get();
        if (c == null) {
            throw new NoContextException("No context set");
        }
        return c;
    }

    /** Open this context for a render pass. Returns an {@link AutoCloseable} that closes it. */
    public OpenScope open() {
        assertMounted();
        if (hookIndex != READY_TO_OPEN || topLevelScope != null) {
            throw new IllegalStateException("RenderContext.open() was already called, and is not reentrant");
        }
        hookIndex = OPENED_AND_UNUSED;

        RenderContext previous = CURRENT.get();
        CURRENT.set(this);

        List<Runnable> oldUnmountListeners = new ArrayList<>(collectedUnmountListeners);
        collectedUnmountListeners.clear();

        List<String> oldContextKeys = new ArrayList<>(collectedContexts);
        collectedContexts.clear();

        collectedEffects.clear();

        // Snapshot the old collected scopes, then start a new set seeded with this render's
        // top-level scope. Objects created directly in the render body register against
        // {@code topLevelScope} via the LivenessScopeStack thread-local.
        Set<LivenessScope> oldScopes = collectedScopes;
        topLevelScope = new LivenessScope();
        collectedScopes = new HashSet<>();
        collectedScopes.add(topLevelScope);
        SafeCloseable scopeHandle = LivenessScopeStack.open(topLevelScope, false);

        return new OpenScope(previous, oldUnmountListeners, oldContextKeys, oldScopes, scopeHandle);
    }

    public final class OpenScope implements AutoCloseable {
        private final RenderContext previous;
        private final List<Runnable> oldUnmountListeners;
        private final List<String> oldContextKeys;
        private final Set<LivenessScope> oldScopes;
        private final SafeCloseable scopeHandle;
        /**
         * Tracks whether the caller marked this render body as failed. Default {@code true} so
         * direct callers (tests) that just open/close get the success-path behavior. The Renderer
         * flips this to {@code false} via {@link #markBodyFailed()} if the body throws, which
         * causes us to retain the old scopes (matching Python's exception path) so any live
         * objects in flight aren't prematurely released.
         */
        private boolean bodySucceeded = true;
        private boolean closed;

        OpenScope(RenderContext previous, List<Runnable> oldUnmountListeners, List<String> oldContextKeys,
                  Set<LivenessScope> oldScopes, SafeCloseable scopeHandle) {
            this.previous = previous;
            this.oldUnmountListeners = oldUnmountListeners;
            this.oldContextKeys = oldContextKeys;
            this.oldScopes = oldScopes;
            this.scopeHandle = scopeHandle;
        }

        /**
         * Mark that the render body raised before producing a result. Called by the Renderer so
         * {@link #close()} keeps old liveness scopes around — they'll be reconciled on the next
         * successful render. Without this, a transient render error would release live objects the
         * surrounding system is still using.
         */
        public void markBodyFailed() {
            bodySucceeded = false;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;

            // Pop our top-level scope off the LivenessScopeStack BEFORE running effects so nested
            // engine calls in effects/cleanups don't accidentally register against it.
            try {
                scopeHandle.close();
            } catch (RuntimeException ignored) {
            }

            try {
                // Run open-context cleanups in reverse registration order (matches Python). These
                // typically pop values pushed by ContextProviderElement so the next sibling sees
                // the right context value.
                for (int i = openCleanups.size() - 1; i >= 0; i--) {
                    try {
                        openCleanups.get(i).run();
                    } catch (RuntimeException ignored) {
                    }
                }
                openCleanups.clear();

                Set<String> currentKeys = new HashSet<>(collectedContexts);
                for (String key : oldContextKeys) {
                    if (!currentKeys.contains(key)) {
                        deleteChildContext(key);
                    }
                }
                Set<Runnable> currentListeners = new HashSet<>(collectedUnmountListeners);
                for (Runnable listener : oldUnmountListeners) {
                    if (!currentListeners.contains(listener)) {
                        try {
                            listener.run();
                        } catch (RuntimeException e) {
                            // continue running other unmount listeners
                        }
                    }
                }
                for (Effect effect : collectedEffects) {
                    if (effect.cleanup != null) {
                        try {
                            effect.cleanup.run();
                        } catch (RuntimeException ignored) {
                        }
                    }
                }
                for (Effect effect : collectedEffects) {
                    if (effect.effect != null) {
                        try {
                            effect.effect.run();
                        } catch (RuntimeException ignored) {
                        }
                    }
                }

                if (bodySucceeded) {
                    // Release scopes that were owned last render but aren't part of this render's
                    // set. Subtract first so a re-used scope's refcount goes 1→2→1 (preserved)
                    // rather than 1→0→1 (released and immediately re-acquired).
                    Set<LivenessScope> toRelease = new HashSet<>(oldScopes);
                    toRelease.removeAll(collectedScopes);
                    for (LivenessScope scope : toRelease) {
                        try {
                            scope.release();
                        } catch (RuntimeException ignored) {
                        }
                    }
                } else {
                    // Body raised: don't release anything. Merge the old set back in so the next
                    // successful render can reconcile.
                    collectedScopes.addAll(oldScopes);
                }

                int seenHooks = hookIndex + 1;
                if (hookCount < 0) {
                    hookCount = seenHooks;
                } else if (hookCount != seenHooks) {
                    throw new IllegalStateException(
                            "Expected to use " + hookCount + " hooks, but used " + seenHooks);
                }
            } finally {
                CURRENT.set(previous);
                if (previous == null) {
                    CURRENT.remove();
                }
                hookIndex = READY_TO_OPEN;
                topLevelScope = null;
                collectedEffects.clear();
            }
        }
    }

    private void assertActive() {
        if (CURRENT.get() != this) {
            throw new IllegalStateException("RenderContext method called when RenderContext not opened");
        }
    }

    private void assertMounted() {
        if (!isMounted) {
            throw new IllegalStateException("RenderContext method called when RenderContext is unmounted");
        }
    }

    public Map<String, List<String>> getQueryParams() {
        return root.getQueryParams();
    }

    public boolean hasState(int key) {
        return state.containsKey(key);
    }

    public Object getState(int key) {
        assertActive();
        return state.get(key);
    }

    /** Read state without requiring the context to be active; used by deferred setters. */
    public Object peekState(int key) {
        return state.get(key);
    }

    public void initState(int key, Object value) {
        if (state.containsKey(key)) {
            throw new IllegalStateException("Key " + key + " is already initialized");
        }
        state.put(key, value);
    }

    public void setState(int key, Object value) {
        if (!state.containsKey(key)) {
            throw new IllegalStateException("Key " + key + " not initialized");
        }
        // Queue the update on the render loop so it's batched with the next render.
        root.onChange(() -> state.put(key, value));
    }

    /** Set state without queueing — used inside an already-scheduled update. */
    public void setStateImmediate(int key, Object value) {
        state.put(key, value);
    }

    public RenderContext getChildContext(String key) {
        RenderContext child = childrenContext.get(key);
        if (child == null) {
            child = new RenderContext(root);
            childrenContext.put(key, child);
        }
        collectedContexts.add(key);
        return child;
    }

    public void deleteChildContext(String key) {
        RenderContext child = childrenContext.remove(key);
        if (child != null) {
            child.unmount();
        }
    }

    public int nextHookIndex() {
        hookIndex++;
        return hookIndex;
    }

    public void queueRender(Runnable update) {
        root.onQueueRender(update);
    }

    public void addEffect(Runnable cleanup, Runnable effect) {
        assertActive();
        collectedEffects.add(new Effect(cleanup, effect));
    }

    public void addUnmountListener(Runnable listener) {
        assertActive();
        collectedUnmountListeners.add(listener);
    }

    /**
     * Register a callback to fire after children of this context have rendered but before unmount
     * listeners / effects run. Used by {@link io.deephaven.ui.element.ContextProviderElement} to
     * pop its value off the {@link io.deephaven.ui.element.UiContext} stack.
     */
    public void addOpenCleanup(Runnable cleanup) {
        assertActive();
        openCleanups.add(cleanup);
    }

    /**
     * Declare that {@code scope} must live until the end of the next successful render of this
     * context. Used by {@code useMemo} and {@code useLivenessScope} to transfer ownership of a
     * derived-object liveness scope to the surrounding render. This context must be active.
     */
    public void manage(LivenessScope scope) {
        assertActive();
        if (scope != null) {
            collectedScopes.add(scope);
        }
    }

    /** @return the scope opened for this render; only non-null while a render is in progress. */
    public LivenessScope topLevelScope() {
        return topLevelScope;
    }

    /**
     * Scopes currently retained by this context. The reference is stable across a render cycle:
     * {@link #open()} replaces the underlying set so a caller holding a reference sees only the
     * old render's set. Exposed primarily for testing and diagnostics.
     */
    public Set<LivenessScope> collectedScopes() {
        return collectedScopes;
    }

    public ExportedRenderState exportState() {
        Map<String, Object> out = new LinkedHashMap<>();

        Map<String, Object> retainedState = new LinkedHashMap<>();
        for (Map.Entry<Integer, Object> entry : state.entrySet()) {
            Object value = entry.getValue();
            if (isRetainable(value)) {
                retainedState.put(entry.getKey().toString(), value);
            }
        }
        if (!retainedState.isEmpty()) {
            out.put("state", retainedState);
        }

        Map<String, Object> childExports = new LinkedHashMap<>();
        for (Map.Entry<String, RenderContext> entry : childrenContext.entrySet()) {
            ExportedRenderState childState = entry.getValue().exportState();
            if (!childState.isEmpty()) {
                childExports.put(entry.getKey(), childState.asMap());
            }
        }
        if (!childExports.isEmpty()) {
            out.put("children", childExports);
        }
        return new ExportedRenderState(out);
    }

    @SuppressWarnings("unchecked")
    public void importState(Map<String, Object> state) {
        this.state.clear();
        this.childrenContext.clear();

        if (state == null) {
            return;
        }
        Object queryParams = state.remove("__queryParams");
        if (queryParams instanceof Map) {
            root.setQueryParams((Map<String, List<String>>) queryParams);
        }
        Object inner = state.get("state");
        if (inner instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) inner).entrySet()) {
                // Keys arrive as strings after JSON deserialization; convert back to int slots.
                this.state.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
        Object children = state.get("children");
        if (children instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) children).entrySet()) {
                if (entry.getValue() instanceof Map) {
                    getChildContext(entry.getKey()).importState((Map<String, Object>) entry.getValue());
                }
            }
        }
    }

    public void unmount() {
        assertMounted();
        isMounted = false;
        for (RenderContext child : childrenContext.values()) {
            child.unmount();
        }
        for (Runnable listener : collectedUnmountListeners) {
            try {
                listener.run();
            } catch (RuntimeException ignored) {
            }
        }
        // Release any scopes still owned so the underlying live objects can drop their refs.
        for (LivenessScope scope : collectedScopes) {
            try {
                scope.release();
            } catch (RuntimeException ignored) {
            }
        }
        collectedScopes.clear();
        hookIndex = READY_TO_OPEN;
        hookCount = -1;
        state.clear();
        childrenContext.clear();
        collectedEffects.clear();
        collectedUnmountListeners.clear();
        collectedContexts.clear();
    }

    private static boolean isRetainable(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean;
    }

    private static final class Effect {
        final Runnable cleanup;
        final Runnable effect;

        Effect(Runnable cleanup, Runnable effect) {
            this.cleanup = cleanup;
            this.effect = effect;
        }
    }
}
