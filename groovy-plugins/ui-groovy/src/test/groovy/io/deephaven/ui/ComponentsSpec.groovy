package io.deephaven.ui

import io.deephaven.ui.element.Element
import io.deephaven.ui.render.NodeEncoder
import io.deephaven.ui.render.RenderContext
import io.deephaven.ui.render.Renderer
import spock.lang.Specification
import spock.lang.Unroll

class ComponentsSpec extends Specification {

    private static Map encode(Element el) {
        def renderer = new Renderer(new RenderContext(new TestRoot()))
        new NodeEncoder().encodeNode(renderer.render(el)).encodedNode
    }

    @Unroll
    def "Ui.#methodName renders as deephaven.ui.components.#wireName"() {
        when:
        def encoded = encode(element)

        then:
        encoded.__dhElemName == "deephaven.ui.components.$wireName"

        where:
        methodName            | wireName                | element
        'accordion'           | 'Accordion'             | Ui.accordion()
        'actionGroup'         | 'ActionGroup'           | Ui.actionGroup()
        'actionMenu'          | 'ActionMenu'            | Ui.actionMenu()
        'avatar'              | 'Avatar'                | Ui.avatar()
        'badge'               | 'Badge'                 | Ui.badge()
        'breadcrumbs'         | 'Breadcrumbs'           | Ui.breadcrumbs()
        'buttonGroup'         | 'ButtonGroup'           | Ui.buttonGroup()
        'calendar'            | 'Calendar'              | Ui.calendar()
        'checkboxGroup'       | 'CheckboxGroup'         | Ui.checkboxGroup()
        'colorPicker'         | 'ColorPicker'           | Ui.colorPicker()
        'comboBox'            | 'ComboBox'              | Ui.comboBox()
        'contextualHelp'      | 'ContextualHelp'        | Ui.contextualHelp()
        'datePicker'          | 'DatePicker'            | Ui.datePicker()
        'dialog'              | 'Dialog'                | Ui.dialog()
        'dialogTrigger'       | 'DialogTrigger'         | Ui.dialogTrigger()
        'disclosure'          | 'Disclosure'            | Ui.disclosure()
        'divider'             | 'Divider'               | Ui.divider()
        'form'                | 'Form'                  | Ui.form()
        'grid'                | 'Grid'                  | Ui.grid()
        'icon'                | 'Icon'                  | Ui.icon()
        'illustratedMessage'  | 'IllustratedMessage'    | Ui.illustratedMessage()
        'image'               | 'Image'                 | Ui.image()
        'inlineAlert'         | 'InlineAlert'           | Ui.inlineAlert()
        'item'                | 'Item'                  | Ui.item()
        'labeledValue'        | 'LabeledValue'          | Ui.labeledValue()
        'link'                | 'Link'                  | Ui.link()
        'listView'            | 'ListView'              | Ui.listView()
        'markdown'            | 'Markdown'              | Ui.markdown()
        'menu'                | 'Menu'                  | Ui.menu()
        'menuTrigger'         | 'MenuTrigger'           | Ui.menuTrigger()
        'meter'               | 'Meter'                 | Ui.meter()
        'numberField'         | 'NumberField'           | Ui.numberField()
        'panel'               | 'Panel'                 | Ui.panel()
        'picker'              | 'Picker'                | Ui.picker()
        'progressBar'         | 'ProgressBar'           | Ui.progressBar()
        'radioGroup'          | 'RadioGroup'            | Ui.radioGroup()
        'rangeSlider'         | 'RangeSlider'           | Ui.rangeSlider()
        'row'                 | 'Row'                   | Ui.row()
        'searchField'         | 'SearchField'           | Ui.searchField()
        'slider'              | 'Slider'                | Ui.slider()
        'tabs'                | 'Tabs'                  | Ui.tabs()
        'textArea'            | 'TextArea'              | Ui.textArea()
        'timeField'           | 'TimeField'             | Ui.timeField()
        'toggleButton'        | 'ToggleButton'          | Ui.toggleButton()
    }

    def "picker accepts items as children"() {
        when:
        def encoded = encode(Ui.picker(label: 'Fruit',
                Ui.item("Apple"), Ui.item("Banana"), Ui.item("Cherry")))

        then:
        encoded.__dhElemName == 'deephaven.ui.components.Picker'
        encoded.props.label == 'Fruit'
        encoded.props.children.size() == 3
        encoded.props.children[0].__dhElemName == 'deephaven.ui.components.Item'
        encoded.props.children[0].props.children == 'Apple'
    }

    def "tabs with tab_list and tab_panels nests correctly"() {
        when:
        def encoded = encode(Ui.tabs(
                Ui.tabList(Ui.item("One", key: "1"), Ui.item("Two", key: "2")),
                Ui.tabPanels(Ui.item("Panel 1", key: "1"), Ui.item("Panel 2", key: "2"))
        ))

        then:
        encoded.__dhElemName == 'deephaven.ui.components.Tabs'
        encoded.props.children.size() == 2
        encoded.props.children[0].__dhElemName == 'deephaven.ui.components.TabList'
        encoded.props.children[1].__dhElemName == 'deephaven.ui.components.TabPanels'
    }

    @Unroll
    def "Html.#tag renders as deephaven.ui.html.#tag"() {
        when:
        def encoded = encode(element)

        then:
        encoded.__dhElemName == "deephaven.ui.html.$tag"

        where:
        tag        | element
        'div'      | Html.div("hello")
        'span'     | Html.span("hi")
        'h1'       | Html.h1("Title")
        'p'        | Html.p("paragraph")
        'a'        | Html.a(href: '/x', "link")
        'ul'       | Html.ul()
        'br'       | Html.br()
        'img'      | Html.img(src: 'x.png')
        'textarea' | Html.textarea()
    }

    def "Html.div composes nested HTML"() {
        when:
        def encoded = encode(Html.div(className: 'wrapper',
                Html.h1("Header"),
                Html.p("Body")
        ))

        then:
        encoded.__dhElemName == 'deephaven.ui.html.div'
        encoded.props.className == 'wrapper'
        encoded.props.children.size() == 2
        encoded.props.children[0].__dhElemName == 'deephaven.ui.html.h1'
        encoded.props.children[1].__dhElemName == 'deephaven.ui.html.p'
    }
}
