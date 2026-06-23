from .BaseTest import ServerTestCase

from deephaven import empty_table, ui

from dh_agent.agent import OutputTab
from dh_agent.ui import _output_tab


class OutputTabTest(ServerTestCase):
    def test_ui_component_renders_directly(self):
        @ui.component
        def c():
            return ui.text("hi")

        element = c()
        tab = _output_tab(OutputTab(key="k", title="t", value=element, kind="ui"))
        self.assertIs(tab.render().get("children"), element)

    def test_dashboard_renders_directly(self):
        dash = ui.dashboard(ui.row(ui.panel(ui.text("a")), ui.panel(ui.text("b"))))
        tab = _output_tab(OutputTab(key="k", title="t", value=dash, kind="ui"))
        self.assertIs(tab.render().get("children"), dash)

    def test_table_is_wrapped_in_ui_table(self):
        out = OutputTab(key="k", title="t", value=empty_table(1), kind="table")
        tab = _output_tab(out)
        self.assertEqual(
            tab.render().get("children").name, "deephaven.ui.elements.UITable"
        )


if __name__ == "__main__":
    import unittest

    unittest.main()
