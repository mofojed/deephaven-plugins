// Dashboard demo: multi-panel layout. Open via the file panel.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

myDashboard = Ui.dashboard(Ui.component { ->
    def (count, setCount) = Ui.useState(0)
    Ui.row(
        Ui.panel(title: 'Controls',
            Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
                Ui.heading("Controls"),
                Ui.text("Count: ${count}"),
                Ui.button("Increment", variant: 'accent', onPress: { setCount(count + 1) }),
                Ui.button("Reset", onPress: { setCount(0) })
            )
        ),
        Ui.panel(title: 'Preview',
            Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
                Ui.heading("Preview"),
                Ui.text(count == 0
                        ? "Click Increment to start."
                        : "You've incremented $count time${count == 1 ? '' : 's'}.")
            )
        )
    )
})

ApplicationContext.get().setField("myDashboard", myDashboard, "Two-panel dashboard demo")
