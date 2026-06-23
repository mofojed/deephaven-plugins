from .BaseTest import ServerTestCase

from deephaven import ui

from dh_agent.ui import _transform_ui, _ui_tab_content


class TransformUiTest(ServerTestCase):
    def test_single_panel_dashboard_unwraps_to_content(self):
        dash = ui.dashboard(ui.panel(ui.text("hi")))
        result = _transform_ui(dash)
        self.assertEqual(result.name, "deephaven.ui.components.Text")

    def test_row_of_panels_becomes_flex(self):
        dash = ui.dashboard(ui.row(ui.panel(ui.text("a")), ui.panel(ui.text("b"))))
        result = _transform_ui(dash)
        self.assertEqual(result.name, "deephaven.ui.components.Flex")
        self.assertEqual(len(result.render().get("children")), 2)

    def test_dashboard_of_stateless_component_unwraps(self):
        # A dashboard whose content is a hook-free component returning panels.
        # The panels are hidden inside the component until rendered; the
        # transform renders it to convert those panels to flex.
        @ui.component
        def board():
            return ui.row(ui.panel(ui.text("a")), ui.panel(ui.text("b")))

        result = _transform_ui(ui.dashboard(board()))
        self.assertEqual(result.name, "deephaven.ui.components.Flex")

    def test_stateful_component_passes_through(self):
        # A component using hooks cannot be rendered without a context, so it is
        # left untouched for the deephaven.ui renderer to handle.
        @ui.component
        def stateful():
            value, _ = ui.use_state(0)
            return ui.text(str(value))

        element = stateful()
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
