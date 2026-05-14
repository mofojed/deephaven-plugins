package io.deephaven.ui

import io.deephaven.ui.element.UiContext
import io.deephaven.ui.hook.Hooks
import io.deephaven.ui.render.NodeEncoder
import io.deephaven.ui.render.RenderContext
import io.deephaven.ui.render.Renderer
import spock.lang.Specification

class ContextSpec extends Specification {

    def "useContext returns the default when no provider is active"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        UiContext<String> Theme = Ui.createContext('light')

        when:
        String value
        ctx.open().withCloseable { value = Hooks.useContext(Theme) }

        then:
        value == 'light'
    }

    def "context provider exposes its value to descendants"() {
        given:
        UiContext<String> Theme = Ui.createContext('light')
        def captured = [:]

        def consumer = Ui.component(name: 'Consumer') { ->
            captured.theme = Ui.useContext(Theme)
            Ui.text("theme=${captured.theme}")
        }
        def app = Ui.component(name: 'App') { ->
            Theme.provider('dark', consumer)
        }
        def renderer = new Renderer(new RenderContext(new TestRoot()))

        when:
        def encoded = new NodeEncoder().encodeNode(renderer.render(app)).encodedNode

        then:
        captured.theme == 'dark'
        // The root is the App user component; the provider wraps the consumer beneath it.
        encoded.__dhElemName == 'App'
        encoded.props.children.__dhElemName == 'deephaven.ui.elements.ContextProviderElement'
    }

    def "nested providers shadow each other"() {
        given:
        UiContext<Integer> Counter = Ui.createContext(0)
        def captured = []

        def inner = Ui.component(name: 'Inner') { ->
            captured << Ui.useContext(Counter)
            Ui.text("inner=${captured.last()}")
        }
        def middle = Ui.component(name: 'Middle') { ->
            Counter.provider(2, inner)
        }
        def app = Ui.component(name: 'App') { ->
            Counter.provider(1, middle)
        }
        def renderer = new Renderer(new RenderContext(new TestRoot()))

        when:
        new NodeEncoder().encodeNode(renderer.render(app))

        then: "innermost provider value wins"
        captured == [2]
    }

    def "stack pops cleanly across sibling providers"() {
        given:
        UiContext<String> Tag = Ui.createContext('default')
        def captured = []

        def consumer = Ui.component(name: 'Consumer') { ->
            captured << Ui.useContext(Tag)
            Ui.text("v=${captured.last()}")
        }
        def app = Ui.component(name: 'App') { ->
            Ui.flex(
                Tag.provider('A', consumer),
                Tag.provider('B', consumer),
                consumer
            )
        }
        def renderer = new Renderer(new RenderContext(new TestRoot()))

        when:
        new NodeEncoder().encodeNode(renderer.render(app))

        then: "first provider sees A, second sees B, third (no provider) sees default"
        captured == ['A', 'B', 'default']
    }
}
