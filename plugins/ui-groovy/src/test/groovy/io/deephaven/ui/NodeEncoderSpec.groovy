package io.deephaven.ui

import io.deephaven.ui.element.RenderedNode
import io.deephaven.ui.render.NodeEncoder
import io.deephaven.ui.render.UiCallable
import spock.lang.Specification

class NodeEncoderSpec extends Specification {

    def "encodes a leaf element with __dhElemName and props"() {
        given:
        def encoder = new NodeEncoder()
        def node = new RenderedNode("deephaven.ui.components.Button", [label: "click"])

        when:
        def result = encoder.encodeNode(node)

        then:
        result.encodedNode.__dhElemName == "deephaven.ui.components.Button"
        result.encodedNode.props == [label: "click"]
        result.newObjects.isEmpty()
    }

    def "assigns cb0, cb1, ... to callables"() {
        given:
        def encoder = new NodeEncoder()
        def cb1 = { "first" } as UiCallable
        def cb2 = { "second" } as UiCallable
        def node = new RenderedNode("deephaven.ui.components.Button", [onPress: cb1, onBlur: cb2])

        when:
        def result = encoder.encodeNode(node)

        then:
        result.encodedNode.props.onPress.__dhCbid == "cb0"
        result.encodedNode.props.onBlur.__dhCbid == "cb1"
        result.liveCallables.size() == 2
    }

    def "stable callable identity reuses the same id"() {
        given:
        def encoder = new NodeEncoder()
        def cb = { "k" } as UiCallable

        when: "two renders with the same callable instance"
        def r1 = encoder.encodeNode(new RenderedNode("X", [onPress: cb]))
        def r2 = encoder.encodeNode(new RenderedNode("X", [onPress: cb]))

        then:
        r1.encodedNode.props.onPress.__dhCbid == "cb0"
        r2.encodedNode.props.onPress.__dhCbid == "cb0"
    }

    def "exports non-serializable objects with __dhObid"() {
        given:
        def encoder = new NodeEncoder()
        def opaque = new Object()
        def node = new RenderedNode("deephaven.ui.components.Table", [source: opaque])

        when:
        def result = encoder.encodeNode(node)

        then:
        result.encodedNode.props.source.__dhObid == 0
        result.newObjects == [opaque]
    }

    def "recursively encodes nested children"() {
        given:
        def encoder = new NodeEncoder()
        def child = new RenderedNode("deephaven.ui.components.Text", [children: "hi"])
        def parent = new RenderedNode("deephaven.ui.components.Flex", [children: [child, "raw"]])

        when:
        def result = encoder.encodeNode(parent)

        then:
        result.encodedNode.__dhElemName == "deephaven.ui.components.Flex"
        result.encodedNode.props.children[0].__dhElemName == "deephaven.ui.components.Text"
        result.encodedNode.props.children[0].props == [children: "hi"]
        result.encodedNode.props.children[1] == "raw"
    }
}
