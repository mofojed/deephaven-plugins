package io.deephaven.ui

import io.deephaven.ui.element.BaseElement
import io.deephaven.ui.element.DashboardElement
import io.deephaven.ui.element.Element
import io.deephaven.ui.element.FunctionElement
import io.deephaven.ui.hook.Hooks
import io.deephaven.ui.hook.LiveHooks
import io.deephaven.ui.hook.Ref
import io.deephaven.ui.hook.StateTuple
import io.deephaven.ui.util.PropCase

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
 *
 * <p>Not yet ported: {@code ui.table}, {@code ui.dashboard}, {@code ui.toast},
 * {@code ui.item_table_source}, and live-data hooks. See plan Phase 2.
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

    /** Get a callable for emitting client-side events. {@code sendEvent.call(name, params)}. */
    static java.util.function.BiConsumer<String, Map<String, Object>> useSendEvent() {
        Hooks.useSendEvent()
    }

    // ─── live-data hooks ──────────────────────────────────────────────────────────────────

    /**
     * Subscribe to a Deephaven Table's updates. The listener fires on the UpdateGraph thread.
     * No-op for null or static tables. Cleanup on unmount or dependency change is automatic.
     */
    static void useTableListener(Object table, Closure listener, List dependencies = []) {
        LiveHooks.useTableListener((io.deephaven.engine.table.Table) table,
                { update, isReplay -> listener.call(update, isReplay) }
                        as java.util.function.BiConsumer,
                dependencies)
    }

    /** Snapshot the table as a list of {@code Map<columnName, value>}. Re-renders on each tick. */
    static List useTableData(Object table) {
        LiveHooks.useTableData((io.deephaven.engine.table.Table) table)
    }

    /** First row of the table (or null if empty). */
    static Map useRowData(Object table) {
        LiveHooks.useRowData((io.deephaven.engine.table.Table) table)
    }

    /** Top-left cell of the table (or null if empty). */
    static Object useCellData(Object table) {
        LiveHooks.useCellData((io.deephaven.engine.table.Table) table)
    }

    /** First column of the table as a list of values. */
    static List useColumnData(Object table) {
        LiveHooks.useColumnData((io.deephaven.engine.table.Table) table)
    }

    // ─── dashboard ─────────────────────────────────────────────────────────────────────────

    /**
     * Wrap an element as the root of a dashboard. The element should render a layout that contains
     * one root row, column, stack, or panel. Server registers this under
     * {@code "deephaven.ui.Dashboard"} so the JS plugin opens the dashboard container.
     */
    static Element dashboard(Element element) {
        new DashboardElement(element)
    }

    // ─── toast ─────────────────────────────────────────────────────────────────────────────

    /**
     * Display a transient toast notification on the client. Must be called on the render thread
     * (i.e., inside a component closure or a callback) — the active event context is found via
     * {@link Hooks#useSendEvent}.
     *
     * <p>Named-arg style: {@code Ui.toast("Saved", variant: 'positive', actionLabel: 'Undo')}.
     * Groovy collapses the named args into the leading {@code Map} per its calling conventions.
     *
     * @param options {@code variant} (neutral|positive|negative|info), {@code actionLabel},
     *                {@code onAction}, {@code shouldCloseOnAction}, {@code onClose}, {@code timeout}, {@code id}.
     * @param message the toast message
     */
    static void toast(Map options, String message) {
        Map params = new LinkedHashMap()
        params.put('message', message)
        // The JS plugin requires variant to be present (matches Python's default of "neutral").
        params.put('variant', 'neutral')
        if (options != null) {
            params.putAll(options)
        }
        // Match the Python plugin: snake_case → camelCase conversion + drop nulls. Lets users pass
        // either action_label or actionLabel.
        Map encoded = PropCase.dictToReactProps(params)
        useSendEvent().accept('toast.event', (Map<String, Object>) encoded)
    }

    /** Plain message toast (no options). */
    static void toast(String message) {
        toast([:], message)
    }

    // ─── components ────────────────────────────────────────────────────────────────────────

    /**
     * Generic constructor for a built-in component. Use this for components not in the dedicated
     * helpers below (e.g., new Spectrum components that ship with the JS bundle).
     */
    static Element componentElement(String name, Map props = [:], Object... children) {
        Map normalized = props == null ? [:] : new LinkedHashMap(props)
        String key = (normalized.remove('key') as String)
        List<Object> childList = (children == null || children.length == 0) ? null : Arrays.asList(children)
        new BaseElement(COMPONENT_NAME_PREFIX + name, childList, key, normalized)
    }

    /**
     * Build an item from a {@link #itemTableSource} (table + column-name annotations) which can be
     * passed as the sole child of {@link #picker}, {@link #comboBox}, or {@link #listView}.
     *
     * @param props {@code keyColumn}, {@code labelColumn}, {@code descriptionColumn},
     *              {@code iconColumn}, {@code titleColumn}, {@code actions}.
     * @param table the Deephaven Table (or {@code PartitionedTable}) supplying the rows.
     */
    static Map itemTableSource(Map props, Object tableArg) {
        Map m = new LinkedHashMap()
        m.put('table', tableArg)
        if (props != null) {
            m.putAll(props)
        }
        return m
    }

    /** {@code Ui.itemTableSource(myTable)} — no column annotations. */
    static Map itemTableSource(Object tableArg) {
        itemTableSource([:], tableArg)
    }

    /**
     * Like {@link #componentElement} but, if the single child is a Map (i.e., from
     * {@link #itemTableSource}), unpacks it: the Map's {@code table} entry becomes the single
     * child and the rest of its entries are merged into the component's props.
     * Mirrors Python's {@code unpack_item_table_source}.
     */
    private static Element unpackingComponentElement(String name, Map props, Object... children) {
        Map mergedProps = (props == null) ? new LinkedHashMap() : new LinkedHashMap(props)
        Object[] mergedChildren = children
        if (children != null && children.length == 1 && children[0] instanceof Map) {
            Map source = new LinkedHashMap((Map) children[0])
            Object table = source.remove('table')
            // Merge the remaining keys (keyColumn / labelColumn / etc.) into props.
            mergedProps.putAll(source)
            mergedChildren = (table == null) ? new Object[0] : [table] as Object[]
        }
        return componentElement(name, mergedProps, mergedChildren)
    }

    static Element accordion(Map props = [:], Object... children) { componentElement('Accordion', props, children) }
    static Element actionButton(Map props = [:], Object... children) { componentElement('ActionButton', props, children) }
    static Element actionGroup(Map props = [:], Object... children) { componentElement('ActionGroup', props, children) }
    static Element actionMenu(Map props = [:], Object... children) { componentElement('ActionMenu', props, children) }
    static Element avatar(Map props = [:], Object... children) { componentElement('Avatar', props, children) }
    static Element badge(Map props = [:], Object... children) { componentElement('Badge', props, children) }
    static Element breadcrumbs(Map props = [:], Object... children) { componentElement('Breadcrumbs', props, children) }
    static Element button(Map props = [:], Object... children) { componentElement('Button', props, children) }
    static Element buttonGroup(Map props = [:], Object... children) { componentElement('ButtonGroup', props, children) }
    static Element calendar(Map props = [:], Object... children) { componentElement('Calendar', props, children) }
    static Element checkbox(Map props = [:], Object... children) { componentElement('Checkbox', props, children) }
    static Element checkboxGroup(Map props = [:], Object... children) { componentElement('CheckboxGroup', props, children) }
    static Element colorEditor(Map props = [:], Object... children) { componentElement('ColorEditor', props, children) }
    static Element colorPicker(Map props = [:], Object... children) { componentElement('ColorPicker', props, children) }
    static Element column(Map props = [:], Object... children) { componentElement('Column', props, children) }
    static Element comboBox(Map props = [:], Object... children) { unpackingComponentElement('ComboBox', props, children) }
    static Element content(Map props = [:], Object... children) { componentElement('Content', props, children) }
    static Element contextualHelp(Map props = [:], Object... children) { componentElement('ContextualHelp', props, children) }
    static Element contextualHelpTrigger(Map props = [:], Object... children) { componentElement('ContextualHelpTrigger', props, children) }
    static Element dateField(Map props = [:], Object... children) { componentElement('DateField', props, children) }
    static Element datePicker(Map props = [:], Object... children) { componentElement('DatePicker', props, children) }
    static Element dateRangePicker(Map props = [:], Object... children) { componentElement('DateRangePicker', props, children) }
    static Element dialog(Map props = [:], Object... children) { componentElement('Dialog', props, children) }
    static Element dialogTrigger(Map props = [:], Object... children) { componentElement('DialogTrigger', props, children) }
    static Element disclosure(Map props = [:], Object... children) { componentElement('Disclosure', props, children) }
    static Element disclosurePanel(Map props = [:], Object... children) { componentElement('DisclosurePanel', props, children) }
    static Element disclosureTitle(Map props = [:], Object... children) { componentElement('DisclosureTitle', props, children) }
    static Element divider(Map props = [:], Object... children) { componentElement('Divider', props, children) }
    static Element flex(Map props = [:], Object... children) { componentElement('Flex', props, children) }
    static Element footer(Map props = [:], Object... children) { componentElement('Footer', props, children) }
    static Element form(Map props = [:], Object... children) { componentElement('Form', props, children) }
    static Element fragment(Map props = [:], Object... children) { componentElement('Fragment', props, children) }
    static Element grid(Map props = [:], Object... children) { componentElement('Grid', props, children) }
    static Element heading(Map props = [:], Object... children) { componentElement('Heading', props, children) }
    static Element icon(Map props = [:], Object... children) { componentElement('Icon', props, children) }
    static Element illustratedMessage(Map props = [:], Object... children) { componentElement('IllustratedMessage', props, children) }
    static Element image(Map props = [:], Object... children) { componentElement('Image', props, children) }
    static Element inlineAlert(Map props = [:], Object... children) { componentElement('InlineAlert', props, children) }
    static Element item(Map props = [:], Object... children) { componentElement('Item', props, children) }
    static Element labeledValue(Map props = [:], Object... children) { componentElement('LabeledValue', props, children) }
    static Element link(Map props = [:], Object... children) { componentElement('Link', props, children) }
    static Element listActionGroup(Map props = [:], Object... children) { componentElement('ListActionGroup', props, children) }
    static Element listActionMenu(Map props = [:], Object... children) { componentElement('ListActionMenu', props, children) }
    static Element listView(Map props = [:], Object... children) { unpackingComponentElement('ListView', props, children) }
    static Element logicButton(Map props = [:], Object... children) { componentElement('LogicButton', props, children) }
    static Element markdown(Map props = [:], Object... children) { componentElement('Markdown', props, children) }
    static Element menu(Map props = [:], Object... children) { componentElement('Menu', props, children) }
    static Element menuTrigger(Map props = [:], Object... children) { componentElement('MenuTrigger', props, children) }
    static Element meter(Map props = [:], Object... children) { componentElement('Meter', props, children) }
    static Element numberField(Map props = [:], Object... children) { componentElement('NumberField', props, children) }
    static Element panel(Map props = [:], Object... children) { componentElement('Panel', props, children) }
    static Element picker(Map props = [:], Object... children) { unpackingComponentElement('Picker', props, children) }
    static Element progressBar(Map props = [:], Object... children) { componentElement('ProgressBar', props, children) }
    static Element progressCircle(Map props = [:], Object... children) { componentElement('ProgressCircle', props, children) }
    static Element radio(Map props = [:], Object... children) { componentElement('Radio', props, children) }
    static Element radioGroup(Map props = [:], Object... children) { componentElement('RadioGroup', props, children) }
    static Element rangeCalendar(Map props = [:], Object... children) { componentElement('RangeCalendar', props, children) }
    static Element rangeSlider(Map props = [:], Object... children) { componentElement('RangeSlider', props, children) }
    static Element row(Map props = [:], Object... children) { componentElement('Row', props, children) }
    static Element searchField(Map props = [:], Object... children) { componentElement('SearchField', props, children) }
    static Element section(Map props = [:], Object... children) { componentElement('Section', props, children) }
    static Element slider(Map props = [:], Object... children) { componentElement('Slider', props, children) }
    static Element stack(Map props = [:], Object... children) { componentElement('Stack', props, children) }
    static Element submenuTrigger(Map props = [:], Object... children) { componentElement('SubmenuTrigger', props, children) }
    /** Renamed from {@code switch} (Groovy reserved word). */
    static Element switch_(Map props = [:], Object... children) { componentElement('Switch', props, children) }
    static Element tab(Map props = [:], Object... children) { componentElement('Tab', props, children) }
    static Element tabList(Map props = [:], Object... children) { componentElement('TabList', props, children) }
    static Element tabPanels(Map props = [:], Object... children) { componentElement('TabPanels', props, children) }
    static Element tabs(Map props = [:], Object... children) { componentElement('Tabs', props, children) }
    static Element tagGroup(Map props = [:], Object... children) { componentElement('TagGroup', props, children) }
    static Element text(Map props = [:], Object... children) { componentElement('Text', props, children) }
    static Element textArea(Map props = [:], Object... children) { componentElement('TextArea', props, children) }
    static Element textField(Map props = [:], Object... children) { componentElement('TextField', props, children) }
    static Element timeField(Map props = [:], Object... children) { componentElement('TimeField', props, children) }
    static Element toggleButton(Map props = [:], Object... children) { componentElement('ToggleButton', props, children) }
    static Element view(Map props = [:], Object... children) { componentElement('View', props, children) }

    // ─── ui.table + formatters ─────────────────────────────────────────────────────────────

    private static final String UI_TABLE_NAME = "deephaven.ui.elements.UITable"

    /**
     * Wrap a Deephaven Table for display with deephaven.ui-specific options.
     *
     * <pre>
     * Ui.table(myTable,
     *     onRowPress: { row -> println row },
     *     format_: [Ui.tableFormat(cols: 'price', value: '0.00')],
     *     showSearch: true)
     * </pre>
     *
     * @param props options like {@code onRowPress}, {@code format_}, {@code quickFilters},
     *              {@code aggregations}, {@code densityKey}, etc.
     * @param tableArg the Deephaven Table (or a URI string / UriElement) to display.
     */
    static Element table(Map props, Object tableArg) {
        Map merged = new LinkedHashMap()
        if (props != null) {
            merged.putAll(props)
        }
        merged.put('table', tableArg)
        String key = (merged.remove('key') as String)
        new BaseElement(UI_TABLE_NAME, null, key, merged)
    }

    /** {@code Ui.table(myTable)} — no extra options. */
    static Element table(Object tableArg) {
        table([:], tableArg)
    }

    /**
     * Aggregation rule for a {@link #table}. Mirrors Python's {@code TableAgg} dataclass.
     *
     * @param props {@code cols} (column or list of columns to aggregate over) and/or
     *              {@code ignoreCols} (mutually exclusive with {@code cols}).
     * @param agg one of: AbsSum, Avg, Count, CountDistinct, Distinct, First, Last,
     *            Max, Min, Std, Sum, Unique, Var.
     */
    static Map tableAgg(Map props, String agg) {
        Map m = new LinkedHashMap()
        m.put('agg', agg)
        if (props != null) {
            m.putAll(props)
        }
        return m
    }

    /** {@code Ui.tableAgg('Sum')} — no extra options. */
    static Map tableAgg(String agg) {
        tableAgg([:], agg)
    }

    /**
     * Formatting rule for a {@link #table}. Mirrors Python's {@code TableFormat} dataclass.
     * Keys: {@code cols}, {@code if_}, {@code color}, {@code backgroundColor}, {@code alignment},
     * {@code value}, {@code mode} (a {@link #tableDatabar}).
     */
    static Map tableFormat(Map props = [:]) {
        Map m = new LinkedHashMap()
        if (props != null) {
            m.putAll(props)
        }
        return m
    }

    /**
     * Databar rendering mode for a column. Mirrors Python's {@code TableDatabar}. Auto-sets
     * {@code type: 'dataBar'} so the JS plugin discriminates it from a heatmap.
     */
    static Map tableDatabar(Map props = [:]) {
        Map m = new LinkedHashMap()
        m.put('type', 'dataBar')
        if (props != null) {
            m.putAll(props)
        }
        return m
    }

    /**
     * Heatmap configuration for a column color. Mirrors Python's {@code TableHeatmap}. Auto-sets
     * {@code type: 'heatmap'} so the JS plugin discriminates it from a databar.
     */
    static Map tableHeatmap(Map props = [:]) {
        Map m = new LinkedHashMap()
        m.put('type', 'heatmap')
        if (props != null) {
            m.putAll(props)
        }
        return m
    }
}
