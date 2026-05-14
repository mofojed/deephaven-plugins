// Demo for the deephaven.ui Groovy backend.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.engine.util.TableTools
import io.deephaven.ui.Ui

sanityTable = TableTools.emptyTable(5).update("X = i")

counter = Ui.component { ->
    def (count, setCount) = Ui.useState(0)
    Ui.flex(direction: 'column', gap: 'size-100',
        Ui.heading("Groovy UI plugin demo"),
        Ui.text("Count: " + count),
        Ui.flex(direction: 'row', gap: 'size-100',
            Ui.button("Increment", onPress: { setCount(count + 1) }),
            Ui.button("Reset", variant: "secondary", onPress: { setCount(0) })
        )
    )
}

// Application Mode requires explicit setField() to expose variables to the IDE's field panel —
// unlike the regular Groovy console where top-level assignments auto-export.
def app = ApplicationContext.get()
app.setField("sanityTable", sanityTable, "Plain Deephaven table sanity check")
app.setField("counter", counter, "deephaven.ui Groovy demo widget")
