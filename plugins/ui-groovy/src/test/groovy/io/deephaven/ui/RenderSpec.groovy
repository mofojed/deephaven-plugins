package io.deephaven.ui

import io.deephaven.ui.element.Element
import io.deephaven.ui.hook.Hooks
import io.deephaven.ui.render.NodeEncoder
import io.deephaven.ui.render.RenderContext
import io.deephaven.ui.render.Renderer
import spock.lang.Specification

class RenderSpec extends Specification {

    def "Ui.text renders a leaf element with snake_case props converted"() {
        given:
        Element el = Ui.text(is_hidden: false, UNSAFE_class_name: 'foo', "hello")
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        def renderer = new Renderer(ctx)

        when:
        def node = renderer.render(el)
        def encoded = new NodeEncoder().encodeNode(node).encodedNode

        then:
        encoded.__dhElemName == "deephaven.ui.components.Text"
        encoded.props.isHidden == false
        encoded.props.UNSAFE_className == 'foo'
        encoded.props.children == 'hello'
    }

    def "Ui.flex with multiple children produces a children list"() {
        given:
        def el = Ui.flex(direction: 'column', Ui.text('a'), Ui.text('b'))
        def root = new TestRoot()
        def renderer = new Renderer(new RenderContext(root))

        when:
        def encoded = new NodeEncoder().encodeNode(renderer.render(el)).encodedNode

        then:
        encoded.props.direction == 'column'
        encoded.props.children.size() == 2
        encoded.props.children[0].__dhElemName == "deephaven.ui.components.Text"
        encoded.props.children[1].__dhElemName == "deephaven.ui.components.Text"
    }

    def "Ui.component renders by invoking the closure"() {
        given:
        def counter = Ui.component { ->
            def (count, _) = Hooks.useState(3)
            Ui.text("Count: $count")
        }
        def renderer = new Renderer(new RenderContext(new TestRoot()))

        when:
        def encoded = new NodeEncoder().encodeNode(renderer.render(counter)).encodedNode

        then:
        encoded.__dhElemName.startsWith("deephaven.ui.user.Component")
        encoded.props.children.__dhElemName == "deephaven.ui.components.Text"
        encoded.props.children.props.children == "Count: 3"
    }

    def "second render after a setState reflects the new value"() {
        given:
        def root = new TestRoot()
        def renderer = new Renderer(new RenderContext(root))
        def encoder = new NodeEncoder()
        def setter = null
        def el = Ui.component { ->
            def (count, set) = Hooks.useState(0)
            setter = set
            Ui.text("$count")
        }

        when:
        encoder.encodeNode(renderer.render(el))
        setter.call(8)
        root.drainRenderQueue()
        def encoded = encoder.encodeNode(renderer.render(el)).encodedNode

        then:
        encoded.props.children.props.children == "8"
    }

    def "button onPress closure is encoded as a callable"() {
        given:
        def cb = { "pressed" }
        def el = Ui.button("Click", onPress: cb)
        def renderer = new Renderer(new RenderContext(new TestRoot()))

        when:
        def encoded = new NodeEncoder().encodeNode(renderer.render(el)).encodedNode

        then:
        encoded.__dhElemName == "deephaven.ui.components.Button"
        encoded.props.onPress.__dhCbid == "cb0"
        encoded.props.children == "Click"
    }
}
