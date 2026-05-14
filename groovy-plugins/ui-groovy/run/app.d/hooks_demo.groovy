// Demos for the routing / context / util hooks.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

// ── useBoolean ────────────────────────────────────────────────────────────────────────────
booleanDemo = Ui.component { ->
    def (isOn, toggle) = Ui.useBoolean(false)
    Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
        Ui.heading("useBoolean demo"),
        Ui.text("Light is: ${isOn ? 'ON' : 'OFF'}"),
        Ui.flex(direction: 'row', gap: 'size-100',
            Ui.button(onPress: { toggle.on() }, "Turn on"),
            Ui.button(onPress: { toggle.off() }, "Turn off"),
            Ui.button(variant: 'accent', onPress: { toggle.toggle() }, "Toggle")
        )
    )
}

// ── useRenderQueue (state update from a background thread) ───────────────────────────────
backgroundDemo = Ui.component { ->
    def (events, setEvents) = Ui.useState([])
    def renderQueue = Ui.useRenderQueue()

    Ui.useEffect({ ->
        def t = new Thread({ ->
            5.times { i ->
                Thread.sleep(1000)
                // Use the render queue to safely schedule a state update from this background
                // thread. The updater form (closure) reads fresh state on the render thread, so
                // the events accumulate even though the original `events` reference is stale.
                renderQueue.accept({ ->
                    setEvents({ List old -> old + ["tick $i"] } as java.util.function.Function)
                } as Runnable)
            }
        })
        t.daemon = true
        t.start()
        return { -> /* let the thread finish naturally for the demo */ } as Runnable
    }, [])

    Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
        Ui.heading("useRenderQueue demo"),
        Ui.text("Events from background thread:"),
        Ui.flex(direction: 'column', gap: 'size-50',
            *events.collect { Ui.text("• $it") }
        )
    )
}

// ── useQueryParam / useSetQueryParam ─────────────────────────────────────────────────────
routingDemo = Ui.component { ->
    def filter = Ui.useQueryParam('filter', 'all')
    def setFilter = Ui.useSetQueryParam('filter')

    Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
        Ui.heading("Routing demo"),
        Ui.text("Open URL with ?filter=value to see it sync; selection updates the URL too."),
        Ui.text("Current filter: ${filter}"),
        Ui.flex(direction: 'row', gap: 'size-100',
            Ui.button(variant: filter == 'all' ? 'accent' : 'primary',
                onPress: { setFilter.set('all') }, "All"),
            Ui.button(variant: filter == 'open' ? 'accent' : 'primary',
                onPress: { setFilter.set('open') }, "Open"),
            Ui.button(variant: filter == 'closed' ? 'accent' : 'primary',
                onPress: { setFilter.set('closed') }, "Closed"),
            Ui.button(onPress: { setFilter.clear() }, "Clear")
        )
    )
}

// ── Context provider / consumer ──────────────────────────────────────────────────────────
def Theme = Ui.createContext('light')

def themedButton = Ui.component(name: 'ThemedButton') { ->
    def theme = Ui.useContext(Theme)
    def bg = (theme == 'dark') ? '#1f1f1f' : '#f0f0f0'
    def fg = (theme == 'dark') ? '#ffffff' : '#000000'
    Ui.view(backgroundColor: bg, padding: 'size-150', borderRadius: 'small',
        Ui.text(UNSAFE_style: [color: fg, fontWeight: 'bold'], "Button in $theme theme")
    )
}

contextDemo = Ui.component { ->
    def (theme, setTheme) = Ui.useState('light')
    Ui.flex(direction: 'column', gap: 'size-200', padding: 'size-200',
        Ui.heading("Context demo"),
        Ui.button(onPress: { setTheme(theme == 'light' ? 'dark' : 'light') },
            "Switch to ${theme == 'light' ? 'dark' : 'light'} theme"),
        Ui.text("All buttons below read the theme via useContext:"),
        Theme.provider(theme,
            Ui.flex(direction: 'column', gap: 'size-100',
                themedButton,
                themedButton,
                themedButton
            )
        )
    )
}

ApplicationContext.get().setField("booleanDemo", booleanDemo, "useBoolean: on/off/toggle setter")
ApplicationContext.get().setField("backgroundDemo", backgroundDemo, "useRenderQueue: state updates from a background thread")
ApplicationContext.get().setField("routingDemo", routingDemo, "useQueryParam / useSetQueryParam round-trip")
ApplicationContext.get().setField("contextDemo", contextDemo, "createContext / Provider / useContext")
