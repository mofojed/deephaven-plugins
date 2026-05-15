// Groovy port of tests/app.d/ui_nested_dashboard.py.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

def nested_dashboard_component = { ->
    Ui.component { ->
        Ui.panel(title: 'Nested Dashboard',
            Ui.dashboard(
                Ui.row(
                    Ui.panel(title: 'Panel A', Ui.text("Content A")),
                    Ui.panel(title: 'Panel B', Ui.text("Content B"))
                )
            )
        )
    }
}

def nested_dashboard_interactive_component = { ->
    Ui.component { ->
        def (count, setCount) = Ui.useState(0)
        Ui.panel(title: 'Interactive Nested Dashboard',
            Ui.dashboard(
                Ui.row(
                    Ui.panel(title: 'Interactive Panel',
                        Ui.button("Clicked ${count} times", onPress: { setCount((count as int) + 1) })),
                    Ui.panel(title: 'Display Panel',
                        Ui.text("Click count: ${count}"))
                )
            )
        )
    }
}

def deeply_nested_dashboard_component = { ->
    Ui.component { ->
        Ui.panel(title: 'Outer Dashboard',
            Ui.dashboard(
                Ui.row(
                    Ui.panel(title: 'Level 1', Ui.text("Content Level 1")),
                    Ui.panel(title: 'Nested Dashboard Container',
                        Ui.dashboard(
                            Ui.row(
                                Ui.panel(title: 'Level 2', Ui.text("Content Level 2")),
                                Ui.panel(title: 'Deepest Panel', Ui.text("Deepest Content"))
                            )
                        )
                    )
                )
            )
        )
    }
}

def nested_dashboard_with_state_component = { ->
    Ui.component { ->
        def (text, setText) = Ui.useState("")
        Ui.panel(title: 'Stateful Nested Dashboard',
            Ui.dashboard(
                Ui.column(
                    Ui.panel(title: 'Input Panel',
                        Ui.textField(label: 'Enter text', value: text, onChange: { v -> setText(v) })),
                    Ui.panel(title: 'Output Panel',
                        Ui.text("You typed: ${text}"))
                )
            )
        )
    }
}

def ui_nested_dashboard = nested_dashboard_component()
def ui_nested_dashboard_interactive = nested_dashboard_interactive_component()
def ui_deeply_nested_dashboard = deeply_nested_dashboard_component()
def ui_nested_dashboard_with_state = nested_dashboard_with_state_component()

def app = ApplicationContext.get()
app.setField("ui_nested_dashboard", ui_nested_dashboard, "Simple nested dashboard")
app.setField("ui_nested_dashboard_interactive", ui_nested_dashboard_interactive, "Interactive nested")
app.setField("ui_deeply_nested_dashboard", ui_deeply_nested_dashboard, "Deeply nested")
app.setField("ui_nested_dashboard_with_state", ui_nested_dashboard_with_state, "Stateful nested")
