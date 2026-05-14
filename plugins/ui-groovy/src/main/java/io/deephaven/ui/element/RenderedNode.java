package io.deephaven.ui.element;

import java.util.Map;

/**
 * Immutable result of rendering a single {@link Element}: an element name plus its rendered props.
 * Walked by {@link io.deephaven.ui.render.NodeEncoder} to emit wire-format JSON.
 */
public final class RenderedNode {

    private final String name;
    private final Map<String, Object> props;

    public RenderedNode(String name, Map<String, Object> props) {
        this.name = name;
        this.props = props;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getProps() {
        return props;
    }
}
