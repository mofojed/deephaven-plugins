package io.deephaven.ui

import io.deephaven.ui.render.RootRenderContext

/**
 * Test-only {@link RootRenderContext} that captures queued state updates and render callables for
 * deterministic inspection. The fake render driver lets tests open contexts, fire state updates,
 * and re-render without spinning up an executor.
 */
class TestRoot implements RootRenderContext {
    List<Runnable> updates = []
    List<Runnable> renderQueue = []
    Map<String, List<String>> queryParams = [:]

    @Override
    void onChange(Runnable stateUpdate) {
        updates << stateUpdate
    }

    @Override
    void onQueueRender(Runnable callable) {
        renderQueue << callable
    }

    @Override
    Map<String, List<String>> getQueryParams() {
        queryParams
    }

    @Override
    void setQueryParams(Map<String, List<String>> queryParams) {
        this.queryParams = queryParams ?: [:]
    }

    /** Flush pending state updates as the render thread would. */
    void flushUpdates() {
        def pending = new ArrayList<Runnable>(updates)
        updates.clear()
        pending.each { it.run() }
    }

    /** Drain the render queue (one shot, not recursive). */
    void drainRenderQueue() {
        def pendingCallables = new ArrayList<Runnable>(renderQueue)
        renderQueue.clear()
        pendingCallables.each { it.run() }
        // Callables may have queued state mutations via onChange; apply those now, mirroring how
        // ElementMessageStream.render() drains updateQueue before the next render.
        flushUpdates()
    }
}
