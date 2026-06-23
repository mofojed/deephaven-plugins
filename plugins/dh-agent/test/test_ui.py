from .BaseTest import ServerTestCase

from deephaven import ui

from dh_agent.ui import _transform_ui, _ui_tab_content


class UiTabContentTest(ServerTestCase):
    def test_single_panel_dashboard_unwraps_to_content(self):
        dash = ui.dashboard(ui.panel(ui.text("hi")))
        result = _transform_ui(dash)
        self.assertEqual(result.name, "deephaven.ui.components.Text")

    def test_row_of_panels_becomes_flex(self):
        dash = ui.dashboard(ui.row(ui.panel(ui.text("a")), ui.panel(ui.text("b"))))
        result = _transform_ui(dash)
        self.assertEqual(result.name, "deephaven.ui.components.Flex")
        self.assertEqual(len(result.render().get("children")), 2)

    def test_plain_component_passes_through(self):
        @ui.component
        def c():
            return ui.text("x")

        element = c()
        self.assertIs(_transform_ui(element), element)

    def test_wrapper_fills_tab(self):
        wrapper = _ui_tab_content(ui.dashboard(ui.panel(ui.text("hi"))))
        props = wrapper.render()
        self.assertEqual(wrapper.name, "deephaven.ui.components.Flex")
        self.assertEqual(props.get("width"), "100%")
        self.assertEqual(props.get("height"), "100%")


if __name__ == "__main__":
    import unittest

    unittest.main()
