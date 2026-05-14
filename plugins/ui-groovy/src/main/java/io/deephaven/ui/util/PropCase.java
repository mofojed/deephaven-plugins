package io.deephaven.ui.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prop-name normalization. Mirrors the Python {@code to_react_prop_case} / {@code dict_to_react_props}
 * helpers so the wire format matches the existing JS plugin exactly.
 *
 * <p>Already-camelCase keys (the common case in idiomatic Groovy code) pass through unchanged.
 * Snake_case keys (Python parity) are converted. {@code UNSAFE_} prefixes are preserved verbatim;
 * {@code aria_} prefixes become {@code aria-}.
 */
public final class PropCase {

    private static final String UNSAFE_PREFIX = "UNSAFE_";
    private static final String ARIA_PREFIX = "aria_";
    private static final String ARIA_REPLACEMENT = "aria-";

    private PropCase() {}

    public static String toCamelCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int leading = 0;
        while (leading < s.length() && s.charAt(leading) == '_') {
            leading++;
        }
        int trailing = 0;
        while (trailing < s.length() - leading && s.charAt(s.length() - 1 - trailing) == '_') {
            trailing++;
        }
        String core = s.substring(leading, s.length() - trailing);
        if (core.isEmpty()) {
            return s;
        }
        String[] parts = core.split("_");
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1));
        }
        return repeat('_', leading) + sb + repeat('_', trailing);
    }

    public static String toReactPropCase(String key) {
        if (key == null) {
            return null;
        }
        if (key.startsWith(UNSAFE_PREFIX)) {
            return UNSAFE_PREFIX + toCamelCase(key.substring(UNSAFE_PREFIX.length()));
        }
        if (key.startsWith(ARIA_PREFIX)) {
            return ARIA_REPLACEMENT + toCamelCase(key.substring(ARIA_PREFIX.length()));
        }
        return toCamelCase(key);
    }

    /** Convert keys + drop null-valued entries. Insertion order preserved. */
    public static Map<String, Object> dictToReactProps(Map<String, Object> props) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (props == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            result.put(toReactPropCase(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static String repeat(char c, int n) {
        if (n <= 0) {
            return "";
        }
        char[] buf = new char[n];
        java.util.Arrays.fill(buf, c);
        return new String(buf);
    }
}
