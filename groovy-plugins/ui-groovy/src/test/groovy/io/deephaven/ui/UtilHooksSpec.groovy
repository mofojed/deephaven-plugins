package io.deephaven.ui

import io.deephaven.ui.hook.BooleanSetter
import io.deephaven.ui.hook.Hooks
import io.deephaven.ui.render.RenderContext
import spock.lang.Specification

class UtilHooksSpec extends Specification {

    def "useBoolean starts at default and toggles via the setter object"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)
        BooleanSetter setter
        Boolean first

        when:
        ctx.open().withCloseable {
            def (val, set) = Hooks.useBoolean(false)
            first = val
            setter = set
        }

        then:
        first == false

        when: "toggle()"
        setter.toggle()
        root.drainRenderQueue()
        Boolean afterToggle
        ctx.open().withCloseable {
            def (val, _) = Hooks.useBoolean(false)
            afterToggle = val
        }

        then:
        afterToggle == true

        when: "off()"
        setter.off()
        root.drainRenderQueue()
        Boolean afterOff
        ctx.open().withCloseable {
            def (val, _) = Hooks.useBoolean(false)
            afterOff = val
        }

        then:
        afterOff == false

        when: "on()"
        setter.on()
        root.drainRenderQueue()
        Boolean afterOn
        ctx.open().withCloseable {
            def (val, _) = Hooks.useBoolean(false)
            afterOn = val
        }

        then:
        afterOn == true
    }

    def "useRenderQueue forwards calls to the root render queue"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)

        when:
        def fired = []
        ctx.open().withCloseable {
            def queue = Hooks.useRenderQueue()
            queue.accept({ -> fired << 'A' } as Runnable)
            queue.accept({ -> fired << 'B' } as Runnable)
        }
        root.drainRenderQueue()

        then:
        fired == ['A', 'B']
    }
}
