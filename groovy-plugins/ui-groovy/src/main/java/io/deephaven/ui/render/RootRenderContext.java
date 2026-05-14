package io.deephaven.ui.render;

import java.util.Map;
import java.util.List;

/**
 * The bridge between a {@link RenderContext} tree and its hosting message stream. Mirrors the
 * Python {@code RootRenderContextProtocol}.
 */
public interface RootRenderContext {

    /** Apply a state update on the render thread before the next render. */
    void onChange(Runnable stateUpdate);

    /** Schedule a render-thread callable; used by hooks like {@code useState}'s setter. */
    void onQueueRender(Runnable callable);

    /** URL query parameters as posted by the client. */
    Map<String, List<String>> getQueryParams();

    /** Update URL query parameters (used by routing hooks; stubbed for MVP). */
    void setQueryParams(Map<String, List<String>> queryParams);
}
