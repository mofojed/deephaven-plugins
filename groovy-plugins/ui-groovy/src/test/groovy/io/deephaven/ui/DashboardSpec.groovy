package io.deephaven.ui

import io.deephaven.ui.element.DashboardElement
import io.deephaven.ui.element.Element
import io.deephaven.ui.objecttype.DashboardType
import io.deephaven.ui.objecttype.ElementType
import io.deephaven.ui.render.NodeEncoder
import io.deephaven.ui.render.RenderContext
import io.deephaven.ui.render.Renderer
import spock.lang.Specification

class DashboardSpec extends Specification {

    def "Ui.dashboard returns a DashboardElement with the correct wire name"() {
        when:
        Element inner = Ui.component { -> Ui.row(Ui.panel(Ui.text("hello"))) }
        Element dash = Ui.dashboard(inner)

        then:
        dash instanceof DashboardElement
        dash.name == 'deephaven.ui.components.Dashboard'
    }

    def "dashboard wraps the inner element as its single child"() {
        given:
        def inner = Ui.component { -> Ui.row(Ui.panel(Ui.text("hello"))) }
        def dash = Ui.dashboard(inner)
        def renderer = new Renderer(new RenderContext(new TestRoot()))

        when:
        def encoded = new NodeEncoder().encodeNode(renderer.render(dash)).encodedNode

        then:
        encoded.__dhElemName == 'deephaven.ui.components.Dashboard'
        encoded.props.children.__dhElemName.startsWith('deephaven.ui.user.Component')
    }

    def "DashboardType.isType picks up dashboard elements and not plain elements"() {
        given:
        def dashType = new DashboardType()
        def elemType = new ElementType()

        when:
        def dash = Ui.dashboard(Ui.component { -> Ui.text("x") })
        def plain = Ui.text("y")

        then:
        dashType.isType(dash)
        !dashType.isType(plain)
        // ElementType is broader: it matches dashboards too. Registration order in UiRegistration
        // ensures DashboardType wins by virtue of being registered first.
        elemType.isType(dash)
        elemType.isType(plain)
    }
}
