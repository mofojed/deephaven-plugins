package io.deephaven.ui

import io.deephaven.ui.hook.Hooks
import io.deephaven.ui.hook.Ref
import io.deephaven.ui.hook.StateTuple
import io.deephaven.ui.render.RenderContext
import spock.lang.Specification

class HooksSpec extends Specification {

    def "useState returns initial value on first render"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)

        when:
        Integer value = null
        ctx.open().withCloseable {
            StateTuple<Integer> tuple = Hooks.useState(42)
            value = tuple.value()
        }

        then:
        value == 42
    }

    def "useState setter queues update and persists across renders"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)

        when: "first render"
        def setter
        Integer firstValue
        ctx.open().withCloseable {
            def (value, set) = Hooks.useState(0)
            firstValue = value as Integer
            setter = set
        }

        then:
        firstValue == 0

        when: "setter is invoked"
        setter.call(7)
        root.drainRenderQueue()

        and: "next render observes the new value"
        Integer secondValue
        ctx.open().withCloseable {
            def (value, _) = Hooks.useState(0)
            secondValue = value as Integer
        }

        then:
        secondValue == 7
    }

    def "useState setter supports updater closures"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        def setter
        ctx.open().withCloseable {
            def (value, set) = Hooks.useState(5)
            setter = set
        }

        when:
        setter.call({ Integer old -> old + 10 })
        root.drainRenderQueue()

        Integer next
        ctx.open().withCloseable {
            def (value, _) = Hooks.useState(5)
            next = value as Integer
        }

        then:
        next == 15
    }

    def "useRef preserves identity across renders"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        Ref<Integer> first
        Ref<Integer> second

        when:
        ctx.open().withCloseable { first = Hooks.useRef(0) }
        first.current = 99
        ctx.open().withCloseable { second = Hooks.useRef(0) }

        then:
        second.is(first)
        second.current == 99
    }

    def "useMemo recomputes only when deps change"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        int invocations = 0
        def deps = [1]

        when:
        Integer a
        ctx.open().withCloseable { a = Hooks.useMemo({ -> invocations++; 7 } as java.util.function.Supplier, deps) }

        and: "render again with same deps"
        Integer b
        ctx.open().withCloseable { b = Hooks.useMemo({ -> invocations++; 7 } as java.util.function.Supplier, deps) }

        then:
        a == 7
        b == 7
        invocations == 1

        when: "render again with new deps"
        Integer c
        ctx.open().withCloseable { c = Hooks.useMemo({ -> invocations++; 99 } as java.util.function.Supplier, [2]) }

        then:
        c == 99
        invocations == 2
    }

    def "calling a hook outside of a render context throws"() {
        when:
        Hooks.useState(0)

        then:
        thrown(io.deephaven.ui.render.NoContextException)
    }
}
