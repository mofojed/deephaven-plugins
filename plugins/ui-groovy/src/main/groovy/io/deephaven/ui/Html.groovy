package io.deephaven.ui

import io.deephaven.ui.element.BaseElement
import io.deephaven.ui.element.Element

/**
 * Raw HTML element constructors. Mirrors the Python {@code ui.html} module.
 *
 * <p>Prefer the Spectrum components on {@link Ui} when possible; this namespace exists for the
 * occasional case where direct HTML markup is needed.
 *
 * <pre>
 * Html.div(className: 'banner',
 *     Html.h1("Hello"),
 *     Html.p("This is a plain HTML block.")
 * )
 * </pre>
 */
class Html {

    private static final String HTML_NAME_PREFIX = "deephaven.ui.html."

    private Html() {}

    /**
     * Generic HTML element. Use this for tags not in the dedicated helpers below.
     */
    static Element htmlElement(String tag, Map attributes = [:], Object... children) {
        Map normalized = attributes == null ? [:] : new LinkedHashMap(attributes)
        String key = (normalized.remove('key') as String)
        List<Object> childList = (children == null || children.length == 0) ? null : Arrays.asList(children)
        new BaseElement(HTML_NAME_PREFIX + tag, childList, key, normalized)
    }

    static Element div(Map attrs = [:], Object... children)      { htmlElement('div', attrs, children) }
    static Element span(Map attrs = [:], Object... children)     { htmlElement('span', attrs, children) }
    static Element h1(Map attrs = [:], Object... children)       { htmlElement('h1', attrs, children) }
    static Element h2(Map attrs = [:], Object... children)       { htmlElement('h2', attrs, children) }
    static Element h3(Map attrs = [:], Object... children)       { htmlElement('h3', attrs, children) }
    static Element h4(Map attrs = [:], Object... children)       { htmlElement('h4', attrs, children) }
    static Element h5(Map attrs = [:], Object... children)       { htmlElement('h5', attrs, children) }
    static Element h6(Map attrs = [:], Object... children)       { htmlElement('h6', attrs, children) }
    static Element p(Map attrs = [:], Object... children)        { htmlElement('p', attrs, children) }
    static Element a(Map attrs = [:], Object... children)        { htmlElement('a', attrs, children) }
    static Element ul(Map attrs = [:], Object... children)       { htmlElement('ul', attrs, children) }
    static Element ol(Map attrs = [:], Object... children)       { htmlElement('ol', attrs, children) }
    static Element li(Map attrs = [:], Object... children)       { htmlElement('li', attrs, children) }
    static Element table(Map attrs = [:], Object... children)    { htmlElement('table', attrs, children) }
    static Element thead(Map attrs = [:], Object... children)    { htmlElement('thead', attrs, children) }
    static Element tbody(Map attrs = [:], Object... children)    { htmlElement('tbody', attrs, children) }
    static Element tr(Map attrs = [:], Object... children)       { htmlElement('tr', attrs, children) }
    static Element th(Map attrs = [:], Object... children)       { htmlElement('th', attrs, children) }
    static Element td(Map attrs = [:], Object... children)       { htmlElement('td', attrs, children) }
    static Element b(Map attrs = [:], Object... children)        { htmlElement('b', attrs, children) }
    static Element i(Map attrs = [:], Object... children)        { htmlElement('i', attrs, children) }
    static Element br(Map attrs = [:], Object... children)       { htmlElement('br', attrs, children) }
    static Element hr(Map attrs = [:], Object... children)       { htmlElement('hr', attrs, children) }
    static Element pre(Map attrs = [:], Object... children)      { htmlElement('pre', attrs, children) }
    static Element code(Map attrs = [:], Object... children)     { htmlElement('code', attrs, children) }
    static Element img(Map attrs = [:], Object... children)      { htmlElement('img', attrs, children) }
    static Element button(Map attrs = [:], Object... children)   { htmlElement('button', attrs, children) }
    static Element input(Map attrs = [:], Object... children)    { htmlElement('input', attrs, children) }
    static Element form(Map attrs = [:], Object... children)     { htmlElement('form', attrs, children) }
    static Element label(Map attrs = [:], Object... children)    { htmlElement('label', attrs, children) }
    static Element select(Map attrs = [:], Object... children)   { htmlElement('select', attrs, children) }
    static Element option(Map attrs = [:], Object... children)   { htmlElement('option', attrs, children) }
    static Element textarea(Map attrs = [:], Object... children) { htmlElement('textarea', attrs, children) }
    static Element style(Map attrs = [:], Object... children)    { htmlElement('style', attrs, children) }
}
