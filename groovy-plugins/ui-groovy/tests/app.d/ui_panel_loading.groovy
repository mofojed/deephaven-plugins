// Groovy port of tests/app.d/ui_panel_loading.py.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

def ui_boom_button = { ->
    Ui.component { ->
        def (value, setValue) = Ui.useState(0)
        if ((value as int) > 0) {
            throw new IllegalArgumentException("BOOM! Value too big.")
        }
        Ui.button("Go BOOM!", onPress: { setValue((value as int) + 1) })
    }
}

def ui_slow_multi_panel_component = { ->
    Ui.component { ->
        def (isMounted, setIsMounted) = Ui.useState(null)
        if (!isMounted) {
            Thread.sleep(1000)
            // Use a complex value (the closure itself) that won't survive a page reload — same
            // sentinel intent as the Python version's `set_is_mounted(ui_boom_button)`.
            setIsMounted(ui_boom_button)
        }
        [
            Ui.panel(Ui.button("Hello")),
            Ui.panel(Ui.text("World")),
            Ui.panel(ui_boom_button())
        ]
    }
}

def ui_slow_multi_panel = ui_slow_multi_panel_component()

ApplicationContext.get().setField("ui_slow_multi_panel", ui_slow_multi_panel, "Slow loading multi-panel")
