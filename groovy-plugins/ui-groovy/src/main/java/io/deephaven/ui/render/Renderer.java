package io.deephaven.ui.render;

import io.deephaven.ui.element.Element;
import io.deephaven.ui.element.RenderedNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks an {@link Element} tree, opening child {@link RenderContext}s as it descends and producing
 * a {@link RenderedNode} tree of plain props. Matches Python's {@code Renderer} semantics:
 * elements appearing as props (e.g. a {@code label}) are rendered recursively, not just children.
 */
public final class Renderer {

    private final RenderContext context;

    public Renderer(RenderContext context) {
        this.context = context;
    }

    public RenderedNode render(Element element) {
        return renderElement(element, context);
    }

    private static RenderedNode renderElement(Element element, RenderContext context) {
        RenderContext.OpenScope scope = context.open();
        boolean success = false;
        try {
            Map<String, Object> props = element.render(context);
            Map<String, Object> renderedProps = renderDictInOpenContext(props, context);
            RenderedNode node = new RenderedNode(element.getName(), renderedProps);
            success = true;
            return node;
        } finally {
            if (!success) {
                scope.markBodyFailed();
            }
            scope.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static Object renderChildItem(Object item, RenderContext parent, String indexKey) {
        if (item instanceof Element) {
            Element element = (Element) item;
            String key = element.getKey() != null ? element.getKey() : indexKey + "-" + element.getName();
            return renderElement(element, parent.getChildContext(key));
        }
        if (item instanceof List) {
            return renderList((List<Object>) item, parent.getChildContext(indexKey));
        }
        if (item instanceof Object[]) {
            return renderList(java.util.Arrays.asList((Object[]) item), parent.getChildContext(indexKey));
        }
        if (item instanceof Map) {
            return renderDict((Map<String, Object>) item, parent.getChildContext(indexKey));
        }
        return item;
    }

    private static List<Object> renderList(List<Object> items, RenderContext context) {
        RenderContext.OpenScope scope = context.open();
        boolean success = false;
        try {
            List<Object> out = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                out.add(renderChildItem(items.get(i), context, String.valueOf(i)));
            }
            success = true;
            return out;
        } finally {
            if (!success) {
                scope.markBodyFailed();
            }
            scope.close();
        }
    }

    private static Map<String, Object> renderDict(Map<String, Object> dict, RenderContext context) {
        RenderContext.OpenScope scope = context.open();
        boolean success = false;
        try {
            Map<String, Object> result = renderDictInOpenContext(dict, context);
            success = true;
            return result;
        } finally {
            if (!success) {
                scope.markBodyFailed();
            }
            scope.close();
        }
    }

    private static Map<String, Object> renderDictInOpenContext(Map<String, Object> dict, RenderContext context) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : dict.entrySet()) {
            out.put(entry.getKey(), renderChildItem(entry.getValue(), context, entry.getKey()));
        }
        return out;
    }
}
