// Groovy port of tests/app.d/ui.py — same exported variable names so the existing
// tests/ui.spec.ts can target the Groovy backend unchanged.

import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

// We wrap each "component" in a Groovy Closure so calling `factory()` creates a fresh Element,
// matching Python's @ui.component decorator semantics. Using the Element directly would still
// work in most places but breaks when a single component appears multiple times in a tree (e.g.
// the dashboard below), since the React key heuristic would collide.

def ui_basic_component = { ->
    Ui.component { ->
        def (count, setCount) = Ui.useState(0)
        def (text, setText) = Ui.useState("hello")

        // The JS plugin invokes onPress with a press-event argument. Use an `_e` placeholder
        // so the closure accepts that arg (a `{ -> ... }` zero-arg closure silently fails when
        // Groovy can't find a matching arity).
        def handlePress = Ui.useCallback({ _e -> setCount({ c -> (c as int) + 1 }) }, [])

        Ui.flex(direction: 'column',
            Ui.actionButton("You pressed me ${count} times", onPress: handlePress),
            Ui.textField(label: "Greeting", value: text, onChange: { v -> setText(v) }),
            Ui.text("You typed ${text}")
        )
    }
}

def ui_multi_panel_component = { ->
    Ui.component { ->
        [
            Ui.panel(title: 'foo', Ui.button("Hello")),
            Ui.panel(title: 'bar', Ui.text("World"))
        ]
    }
}

def ui_boom_component = { ->
    Ui.component { ->
        throw new RuntimeException("BOOM!")
    }
}

def ui_boom_counter_component = { ->
    Ui.component { ->
        def (value, setValue) = Ui.useState(0)
        if ((value as int) > 1) {
            throw new IllegalArgumentException("BOOM! Value too big.")
        }
        Ui.button("Count is ${value}", onPress: { setValue((value as int) + 1) })
    }
}

def ui_cell = { Map opts = [:] ->
    Ui.component { ->
        def label = (opts.label as String) ?: "Cell"
        def (text, setText) = Ui.useState("")
        Ui.textField(label: label, value: text, onChange: { v -> setText(v) })
    }
}

def ui_cells_component = { ->
    Ui.component { ->
        // AtomicInteger is the JVM stand-in for itertools.count() — survives across renders.
        def idIter = Ui.useState({ -> new java.util.concurrent.atomic.AtomicInteger(0) } as java.util.function.Supplier).value()
        def (cells, setCells) = Ui.useState({ -> [idIter.getAndIncrement()] } as java.util.function.Supplier)

        def addCell = { -> setCells({ old -> (old as List) + [idIter.getAndIncrement()] }) }
        def deleteCell = { int del -> setCells({ old -> (old as List).findAll { it != del } }) }

        Ui.view(overflow: 'auto',
            (cells as List).collect { i ->
                Ui.flex(alignItems: 'end', key: String.valueOf(i),
                    ui_cell(label: "Cell ${i}"),
                    Ui.actionButton(ariaLabel: 'Delete cell', onPress: { deleteCell(i as int) },
                        Ui.icon("trash"))
                )
            },
            Ui.actionButton(onPress: { addCell() },
                Ui.icon("add"),
                "Add cell")
        )
    }
}

def ui_component = ui_basic_component()
def ui_multi_panel = ui_multi_panel_component()
def ui_boom = ui_boom_component()
def ui_boom_counter = ui_boom_counter_component()
def ui_cells = ui_cells_component()

def ui_dashboard = Ui.dashboard(
    Ui.column(
        Ui.stack(activeItemIndex: 0, height: 75,
            Ui.panel(title: 'Component', ui_basic_component()),
            Ui.panel(title: 'Boom Counter', ui_boom_counter_component())
        ),
        Ui.row(height: 25,
            ui_multi_panel_component()
        )
    )
)

// Groovy Application Mode does NOT auto-export top-level vars (verified empirically: server
// reports 0 exports without explicit setField). Each fixture below needs an explicit setField
// to surface in the IDE's panel list — same gotcha #11 from plans/deephaven-ui-groovy.md.
def app = ApplicationContext.get()
app.setField("ui_component", ui_component, "Basic UI test")
app.setField("ui_multi_panel", ui_multi_panel, "Multi-panel test")
app.setField("ui_boom", ui_boom, "Error component test")
app.setField("ui_boom_counter", ui_boom_counter, "Error-on-click test")
app.setField("ui_cells", ui_cells, "List-with-keys test")
app.setField("ui_dashboard", ui_dashboard, "Dashboard test")
