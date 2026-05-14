package io.deephaven.ui

import io.deephaven.ui.event.EventContext
import io.deephaven.ui.render.NoContextException
import spock.lang.Specification

class ToastSpec extends Specification {

    def "Ui.toast fires an event on the active EventContext"() {
        given:
        def events = []
        def ctx = new EventContext({ name, params -> events << [name: name, params: params] })

        when:
        ctx.open().withCloseable {
            Ui.toast("Hello world")
        }

        then:
        events.size() == 1
        events[0].name == 'toast.event'
        events[0].params.message == 'Hello world'
    }

    def "Ui.toast forwards options with snake_case to camelCase conversion"() {
        given:
        def events = []
        def ctx = new EventContext({ name, params -> events << params })
        def closeHandler = { -> }

        when:
        ctx.open().withCloseable {
            Ui.toast("Saved",
                    variant: 'positive',
                    action_label: 'Undo',
                    on_close: closeHandler,
                    timeout: 5000)
        }

        then:
        events.size() == 1
        events[0].message == 'Saved'
        events[0].variant == 'positive'
        events[0].actionLabel == 'Undo'
        events[0].timeout == 5000
        events[0].onClose.is(closeHandler)
    }

    def "Ui.toast without an active EventContext throws NoContextException"() {
        when:
        Ui.toast("nope")

        then:
        thrown(NoContextException)
    }

    def "useSendEvent returns a BiConsumer routing through the current context"() {
        given:
        def caught = [:]
        def ctx = new EventContext({ name, params -> caught.name = name; caught.params = params })

        when:
        ctx.open().withCloseable {
            def send = Ui.useSendEvent()
            send.accept('my.event', [foo: 'bar'])
        }

        then:
        caught.name == 'my.event'
        caught.params == [foo: 'bar']
    }
}
