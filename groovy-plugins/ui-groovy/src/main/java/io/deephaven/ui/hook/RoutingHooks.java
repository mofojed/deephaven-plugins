package io.deephaven.ui.hook;

import io.deephaven.ui.event.EventContext;
import io.deephaven.ui.render.RenderContext;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * URL query-parameter hooks. Mirrors Python's {@code use_query_params},
 * {@code use_query_param}, and {@code use_set_query_param}. The setter fires a
 * {@code "navigate.event"} JS event with a percent-encoded query string.
 */
public final class RoutingHooks {

    private static final String NAVIGATE_EVENT = "navigate.event";

    private RoutingHooks() {}

    /**
     * The full URL query-parameter map as posted by the client. Keys are parameter names; values
     * are lists of string values (since query strings can repeat a key).
     */
    public static Map<String, List<String>> useQueryParams() {
        return RenderContext.current().getQueryParams();
    }

    /**
     * Read a single query parameter.
     *
     * <p>If {@code defaultValue} is omitted (null), returns the LAST value as a String, or
     * {@code null} if the key is missing. If {@code defaultValue} is a list, returns the full
     * list of values (or the default list if the key is absent).
     */
    public static String useQueryParam(String key) {
        return useQueryParam(key, (String) null);
    }

    public static String useQueryParam(String key, String defaultValue) {
        Map<String, List<String>> params = useQueryParams();
        List<String> values = params.get(key);
        if (values == null) {
            return defaultValue;
        }
        return values.isEmpty() ? "" : values.get(values.size() - 1);
    }

    public static List<String> useQueryParam(String key, List<String> defaultValue) {
        Map<String, List<String>> params = useQueryParams();
        List<String> values = params.get(key);
        return values == null ? defaultValue : values;
    }

    /**
     * Returns a setter for a single query parameter. Calling the setter with {@code null} or an
     * empty list removes the key; with a String it replaces it; with a List<String> it sets the
     * full value list. The second arg ({@code replace}, default true) controls whether browser
     * history is replaced or pushed.
     */
    public static SetQueryParam useSetQueryParam(String key) {
        Map<String, List<String>> current = useQueryParams();
        BiConsumer<String, Map<String, Object>> sendEvent = Hooks.useSendEvent();
        return (value, replace) -> {
            Map<String, List<String>> next = new LinkedHashMap<>(current);
            if (value == null) {
                next.remove(key);
            } else if (value instanceof String) {
                next.put(key, Collections.singletonList((String) value));
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = new ArrayList<>((List<String>) value);
                if (list.isEmpty()) {
                    next.remove(key);
                } else {
                    next.put(key, list);
                }
            } else {
                throw new IllegalArgumentException(
                        "useSetQueryParam setter expects null, String, or List<String>; got "
                                + value.getClass().getName());
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("queryParams", encodeQueryString(next));
            payload.put("replace", replace);
            sendEvent.accept(NAVIGATE_EVENT, payload);
        };
    }

    /** Setter signature for {@link #useSetQueryParam}; accepts (value, replace?). */
    @FunctionalInterface
    public interface SetQueryParam {
        void set(Object value, boolean replace);

        default void set(Object value) {
            set(value, true);
        }

        /** Clear the parameter. */
        default void clear() {
            set(null, true);
        }
    }

    private static String encodeQueryString(Map<String, List<String>> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, List<String>> e : params.entrySet()) {
            for (String v : e.getValue()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}
