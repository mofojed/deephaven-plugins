package io.deephaven.ui

import io.deephaven.ui.element.Element
import io.deephaven.ui.render.NodeEncoder
import io.deephaven.ui.render.RenderContext
import io.deephaven.ui.render.Renderer
import spock.lang.Specification

class TableComponentSpec extends Specification {

    private static Map encode(Element el) {
        def renderer = new Renderer(new RenderContext(new TestRoot()))
        new NodeEncoder().encodeNode(renderer.render(el)).encodedNode
    }

    def "Ui.table uses deephaven.ui.elements.UITable name and exports table as object"() {
        given:
        def fakeTable = new Object()
        def el = Ui.table(fakeTable)

        when:
        def result = new Renderer(new RenderContext(new TestRoot())).render(el)
        def encoderResult = new NodeEncoder().encodeNode(result)
        def encoded = encoderResult.encodedNode

        then:
        encoded.__dhElemName == 'deephaven.ui.elements.UITable'
        // The Table is non-primitive — encoder makes it an exported object reference.
        encoded.props.table.__dhObid == 0
        encoderResult.newObjects == [fakeTable]
    }

    def "Ui.table forwards additional props"() {
        given:
        def fakeTable = new Object()

        when:
        def encoded = encode(Ui.table(showSearch: true, density: 'compact', fakeTable))

        then:
        encoded.props.showSearch == true
        encoded.props.density == 'compact'
        encoded.props.table.__dhObid == 0
    }

    def "Ui.tableAgg returns a Map with the agg key"() {
        expect:
        Ui.tableAgg('Sum') == [agg: 'Sum']
        Ui.tableAgg(cols: ['price', 'qty'], 'Avg') == [agg: 'Avg', cols: ['price', 'qty']]
    }

    def "Ui.tableDatabar / tableHeatmap include the type discriminator"() {
        expect:
        Ui.tableDatabar() == [type: 'dataBar']
        Ui.tableDatabar(min: 0, max: 100) == [type: 'dataBar', min: 0, max: 100]
        Ui.tableHeatmap() == [type: 'heatmap']
        Ui.tableHeatmap(min: 0, max: 100, mid: 50) == [type: 'heatmap', min: 0, max: 100, mid: 50]
    }

    def "Ui.tableFormat passes props through"() {
        expect:
        Ui.tableFormat(cols: 'price', backgroundColor: 'red') ==
                [cols: 'price', backgroundColor: 'red']
    }

    def "Ui.itemTableSource builds a Map with the table key"() {
        given:
        def fakeTable = new Object()

        when:
        def src = Ui.itemTableSource(keyColumn: 'id', labelColumn: 'name', fakeTable)

        then:
        src == [table: fakeTable, keyColumn: 'id', labelColumn: 'name']
    }

    def "Ui.picker unpacks an itemTableSource child"() {
        given:
        def fakeTable = new Object()
        def src = Ui.itemTableSource(keyColumn: 'id', labelColumn: 'name', fakeTable)

        when:
        def encoded = encode(Ui.picker(label: 'Pick', src))

        then:
        encoded.__dhElemName == 'deephaven.ui.components.Picker'
        encoded.props.label == 'Pick'
        encoded.props.keyColumn == 'id'
        encoded.props.labelColumn == 'name'
        // The unpacked table becomes the single child — encoded as an exported object.
        encoded.props.children.__dhObid == 0
    }

    def "Ui.picker with regular Item children passes them through unchanged"() {
        when:
        def encoded = encode(Ui.picker(label: 'Pick',
                Ui.item("Apple"), Ui.item("Banana")))

        then:
        encoded.props.label == 'Pick'
        encoded.props.children.size() == 2
        encoded.props.children[0].__dhElemName == 'deephaven.ui.components.Item'
    }

    def "Ui.comboBox and Ui.listView also unpack itemTableSource"() {
        given:
        def fakeTable = new Object()
        def src = Ui.itemTableSource(keyColumn: 'id', fakeTable)

        when:
        def cbEncoded = encode(Ui.comboBox(label: 'CB', src))
        def lvEncoded = encode(Ui.listView(label: 'LV', src))

        then:
        cbEncoded.props.keyColumn == 'id'
        cbEncoded.props.children.__dhObid != null
        lvEncoded.props.keyColumn == 'id'
        lvEncoded.props.children.__dhObid != null
    }
}
