package io.deephaven.ui

import groovy.transform.CompileStatic
import io.deephaven.ui.element.BaseElement
import io.deephaven.ui.element.Element
import io.deephaven.ui.element.FunctionElement
import io.deephaven.ui.hook.Hooks
import io.deephaven.ui.hook.Ref
import io.deephaven.ui.hook.StateTuple

/**
 * Public Groovy API for the deephaven.ui plugin. Mirrors the Python {@code import deephaven.ui as ui}
 * surface so existing Python examples translate directly:
 *
 * <pre>
 * def counter = Ui.component { ->
 *     def (count, setCount) = Ui.useState(0)
 *     Ui.flex(direction: 'column',
 *         Ui.text("Count: $count"),
 *         Ui.button("Increment", onPress: { setCount(count + 1) })
 *     )
 * }
 * </pre>
 *
 * <p>Component prop names may be passed as either camelCase ({@code onPress}, idiomatic Groovy)
 * or snake_case ({@code on_press}, Python parity) — both produce identical wire output.
 */
class Ui {

    private static int nextUserComponentId = 0
    private static final String COMPONENT_NAME_PREFIX = "deephaven.ui.components."
    private static final String USER_NAME_PREFIX = "deephaven.ui.user."

    private Ui() {}

    // ─── component decorator ───────────────────────────────────────────────────────────────

    /** Wrap a render closure into an {@link Element}. */
    static Element component(Closure body) {
        component([:], body)
    }

    /**
     * Wrap a render closure into an {@link Element}.
     *
     * @param opts {@code name:} optional component name; {@code key:} optional React key.
     */
    static Element component(Map opts, Closure body) {
        String name = (opts?.name as String) ?: nextUserComponentName()
        String key = opts?.key as String
        new FunctionElement(name, { -> body.call() }, key)
    }

    private static synchronized String nextUserComponentName() {
        USER_NAME_PREFIX + "Component" + (nextUserComponentId++)
    }

    // ─── hooks (delegate to Java) ──────────────────────────────────────────────────────────

    static <T> StateTuple<T> useState(T initial) { Hooks.useState(initial) }
    static <T> Ref<T> useRef(T initial = null) { Hooks.useRef(initial) }
    static <T> T useMemo(Closure<T> fn, List dependencies) { Hooks.useMemo({ -> fn.call() }, dependencies) }
    static <T> T useCallback(T callback, List dependencies) { Hooks.useCallback(callback, dependencies) }
    static void useEffect(Closure effect, List dependencies) {
        Hooks.useEffect({ ->
            def result = effect.call()
            (result instanceof Runnable) ? (Runnable) result : null
        }, dependencies)
    }

    // ─── components ────────────────────────────────────────────────────────────────────────

    /**
     * Generic constructor for a built-in component. Used by every built-in below; users can also
     * call directly for components not in the MVP set (the JS plugin still ships the full React
     * Spectrum component map).
     */
    static Element componentElement(String name, Map props = [:], Object... children) {
        Map normalized = props == null ? [:] : new LinkedHashMap(props)
        String key = (normalized.remove('key') as String)
        List<Object> childList = (children == null || children.length == 0) ? null : Arrays.asList(children)
        new BaseElement(COMPONENT_NAME_PREFIX + name, childList, key, normalized)
    }

    static Element text(Map props = [:], Object... children) { componentElement('Text', props, children) }
    static Element heading(Map props = [:], Object... children) { componentElement('Heading', props, children) }
    static Element button(Map props = [:], Object... children) { componentElement('Button', props, children) }
    static Element actionButton(Map props = [:], Object... children) { componentElement('ActionButton', props, children) }
    static Element flex(Map props = [:], Object... children) { componentElement('Flex', props, children) }
    static Element view(Map props = [:], Object... children) { componentElement('View', props, children) }
    static Element fragment(Map props = [:], Object... children) { componentElement('Fragment', props, children) }
    static Element textField(Map props = [:], Object... children) { componentElement('TextField', props, children) }
    static Element checkbox(Map props = [:], Object... children) { componentElement('Checkbox', props, children) }
    /** Renamed from {@code switch} (Groovy reserved word). */
    static Element switch_(Map props = [:], Object... children) { componentElement('Switch', props, children) }
}
