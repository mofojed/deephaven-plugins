package io.deephaven.ui

import io.deephaven.ui.event.EventContext
import io.deephaven.ui.hook.RoutingHooks
import io.deephaven.ui.render.RenderContext
import spock.lang.Specification

class RoutingHooksSpec extends Specification {

    def "useQueryParams exposes the current URL params from the root"() {
        given:
        def root = new TestRoot()
        root.queryParams = ['page': ['3'], 'tag': ['python', 'java']]
        def ctx = new RenderContext(root)

        when:
        Map<String, List<String>> params
        ctx.open().withCloseable { params = RoutingHooks.useQueryParams() }

        then:
        params == ['page': ['3'], 'tag': ['python', 'java']]
    }

    def "useQueryParam returns the last value as a String"() {
        given:
        def root = new TestRoot()
        root.queryParams = ['tag': ['python', 'java']]
        def ctx = new RenderContext(root)

        when:
        String tag
        ctx.open().withCloseable { tag = RoutingHooks.useQueryParam('tag') }

        then:
        tag == 'java'
    }

    def "useQueryParam returns the default when the key is absent"() {
        given:
        def root = new TestRoot()
        def ctx = new RenderContext(root)

        when:
        String missing
        ctx.open().withCloseable { missing = RoutingHooks.useQueryParam('nothing', 'fallback') }

        then:
        missing == 'fallback'
    }

    def "useQueryParam with a list default returns the full list"() {
        given:
        def root = new TestRoot()
        root.queryParams = ['tag': ['python', 'java']]
        def ctx = new RenderContext(root)

        when:
        List<String> tags
        ctx.open().withCloseable { tags = RoutingHooks.useQueryParam('tag', [] as List<String>) }

        then:
        tags == ['python', 'java']
    }

    def "useSetQueryParam fires a navigate.event with the encoded query string"() {
        given:
        def root = new TestRoot()
        root.queryParams = ['x': ['1']]
        def ctx = new RenderContext(root)
        def events = []
        def ec = new EventContext({ name, params -> events << [name: name, params: params] })

        when:
        RoutingHooks.SetQueryParam setter
        ctx.open().withCloseable {
            ec.open().withCloseable {
                setter = RoutingHooks.useSetQueryParam('x')
                setter.set('42')
            }
        }

        then:
        events.size() == 1
        events[0].name == 'navigate.event'
        events[0].params.queryParams == '?x=42'
        events[0].params.replace == true
    }

    def "useSetQueryParam with null removes the key"() {
        given:
        def root = new TestRoot()
        root.queryParams = ['x': ['1'], 'y': ['2']]
        def ctx = new RenderContext(root)
        def events = []
        def ec = new EventContext({ name, params -> events << params })

        when:
        ctx.open().withCloseable {
            ec.open().withCloseable {
                def setter = RoutingHooks.useSetQueryParam('x')
                setter.set(null)
            }
        }

        then:
        events[0].queryParams == '?y=2'
    }
}
