from .BaseTest import ServerTestCase

from deephaven import ui

from dh_agent.agent import OutputTab
from dh_agent.ui import (
    _dashboard_layout_child,
    _is_dashboard_layout,
    _split_outputs,
    _ui_tab_content,
)


def _ui_output(value, key="k", title="t"):
    return OutputTab(key=key, title=title, value=value, kind="ui")


class DashboardLayoutSplitTest(ServerTestCase):
    def test_dashboard_is_layout_output(self):
        self.assertTrue(_is_dashboard_layout(ui.dashboard(ui.panel(ui.text("hi")))))

    def test_panel_is_layout_output(self):
        self.assertTrue(_is_dashboard_layout(ui.panel(ui.text("hi"))))

    def test_plain_component_is_not_layout_output(self):
        @ui.component
        def c():
            return ui.text("x")

        self.assertFalse(_is_dashboard_layout(c()))

    def test_split_routes_dashboards_to_layout(self):
        dash = _ui_output(ui.dashboard(ui.panel(ui.text("hi"))), key="d")

        @ui.component
        def c():
            return ui.text("x")

        comp = _ui_output(c(), key="c")
        table_out = OutputTab(key="t", title="t", value=object(), kind="table")

        layout, tabbed = _split_outputs([dash, comp, table_out])

        self.assertEqual([o.key for o in layout], ["d"])
        self.assertEqual([o.key for o in tabbed], ["c", "t"])


class DashboardLayoutChildTest(ServerTestCase):
    def test_dashboard_unwraps_to_inner_layout(self):
        inner = ui.row(ui.panel(ui.text("a")), ui.panel(ui.text("b")))
        child = _dashboard_layout_child(_ui_output(ui.dashboard(inner)))
        self.assertEqual(child.name, "deephaven.ui.components.Row")

    def test_dashboard_with_component_inner_wraps_in_panel(self):
        @ui.component
        def board():
            return ui.row(ui.panel(ui.text("a")), ui.panel(ui.text("b")))

        child = _dashboard_layout_child(_ui_output(ui.dashboard(board())))
        # A FunctionElement inner must be wrapped so it is a valid panel child.
        self.assertEqual(child.name, "deephaven.ui.components.Panel")

    def test_panel_passes_through(self):
        panel = ui.panel(ui.text("hi"))
        self.assertIs(_dashboard_layout_child(_ui_output(panel)), panel)


class UiTabContentTest(ServerTestCase):
    def test_wrapper_fills_tab(self):
        @ui.component
        def c():
            return ui.text("hi")

        wrapper = _ui_tab_content(c())
        props = wrapper.render()
        self.assertEqual(wrapper.name, "deephaven.ui.components.Flex")
        self.assertEqual(props.get("width"), "100%")
        self.assertEqual(props.get("height"), "100%")


if __name__ == "__main__":
    import unittest

    unittest.main()
