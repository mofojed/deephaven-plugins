// Exercises components added in the second pass.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Html
import io.deephaven.ui.Ui

showcase = Ui.component { ->
    def (name, setName) = Ui.useState("World")
    def (pick, setPick) = Ui.useState("apple")
    def (toggle, setToggle) = Ui.useState(false)
    def (count, setCount) = Ui.useState(0)

    Ui.flex(direction: 'column', gap: 'size-200', padding: 'size-200',
        Ui.heading(level: 2, "deephaven.ui Groovy showcase"),
        Ui.divider(),

        // Form inputs
        Ui.form(
            Ui.textField(label: 'Name', value: name, onChange: { v -> setName(v) }),
            Ui.numberField(label: 'Count', value: count, onChange: { v -> setCount(v) }),
            Ui.switch_(isSelected: toggle, onChange: { v -> setToggle(v) }, "Toggle me"),
            Ui.picker(label: 'Fruit', selectedKey: pick, onSelectionChange: { v -> setPick(v) },
                Ui.item("Apple", key: 'apple'),
                Ui.item("Banana", key: 'banana'),
                Ui.item("Cherry", key: 'cherry')
            )
        ),

        Ui.divider(),

        // Live preview
        Ui.view(backgroundColor: 'gray-100', padding: 'size-200', borderRadius: 'small',
            Ui.text("Hello, ${name}!"),
            Ui.text("Count: ${count}"),
            Ui.text("Toggle: ${toggle}"),
            Ui.text("Pick: ${pick}")
        ),

        Ui.divider(),

        // Buttons + badge
        Ui.flex(direction: 'row', gap: 'size-100', alignItems: 'center',
            Ui.button(variant: 'accent', onPress: { setCount(count + 1) }, "Increment"),
            Ui.actionButton(onPress: { setCount(0) }, "Reset"),
            Ui.toggleButton(isSelected: toggle, onChange: { v -> setToggle(v) }, "Toggle Btn"),
            Ui.badge(variant: 'positive', "${count} clicks")
        ),

        Ui.divider(),

        // Progress + meter
        Ui.flex(direction: 'column', gap: 'size-100',
            Ui.progressBar(label: 'Loading...', value: Math.min(count * 10, 100)),
            Ui.meter(label: 'Capacity', value: Math.min(count * 5, 100), variant: 'positive')
        ),

        Ui.divider(),

        // Raw HTML
        Html.div(style: [padding: '8px', border: '1px dashed gray'],
            Html.h3("Raw HTML works too"),
            Html.p("This block is rendered via Html.div / Html.h3 / Html.p.")
        )
    )
}

ApplicationContext.get().setField("showcase", showcase, "deephaven.ui Groovy showcase: full-feature demo")
