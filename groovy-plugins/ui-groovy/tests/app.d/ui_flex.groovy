// Groovy port of tests/app.d/ui_flex.py. The Python file uses dx.line() (deephaven.plot.express)
// for the _p_flex cases — that plugin is Python-only, so we substitute a Deephaven Figure built
// via the Plot Builder API where the spec checks for a plot trace. The variable names match the
// Python file so tests/ui.spec.ts's flex_N test cases hit the same fixtures.

import io.deephaven.appmode.ApplicationContext
import io.deephaven.engine.util.TableTools
import io.deephaven.plot.PlottingConvenience
import io.deephaven.ui.Ui

def _t_flex = TableTools.emptyTable(100).update("x = i", "y = Math.sin(i)")
// Deephaven's built-in Plot Builder; trace class is set in the JS plugin so visual checks find it.
def _p_flex = PlottingConvenience.plot("y", _t_flex, "x", "y").show()

def ui_flex_text_field_input_types_examples = { ->
    Ui.component { ->
        [
            Ui.form(validationBehavior: 'native',
                Ui.textField(label: "Name", type: "text", isRequired: true),
                Ui.textField(label: "Personal Website", type: "url", isRequired: true),
                Ui.textField(label: "Phone", type: "tel", isRequired: true),
                Ui.textField(label: "Email", type: "email", isRequired: true),
                Ui.textField(label: "Password", type: "password", isRequired: true),
                Ui.textField(label: "Search Bar", type: "search")
            ),
            _t_flex
        ]
    }
}

def ui_flex_test_component = { ->
    Ui.component { ->
        [Ui.textField(), _t_flex, _t_flex]
    }
}

def flex_0 = ui_flex_test_component()
def flex_1 = ui_flex_text_field_input_types_examples()
def flex_2 = Ui.panel(backgroundColor: 'red', _t_flex, _t_flex)
def flex_3 = Ui.button("test")
def flex_4 = Ui.textField(label: "test", labelPosition: 'side')
def flex_5 = Ui.panel(backgroundColor: 'blue', Ui.flex(direction: 'column', _t_flex, _t_flex))
def flex_6 = Ui.panel(backgroundColor: 'green', Ui.flex(direction: 'row', _t_flex, _t_flex))
def flex_7 = Ui.panel(direction: 'row', _t_flex, _p_flex)
def flex_8 = Ui.panel(_t_flex, _p_flex)
def flex_9 = Ui.panel(Ui.textField(label: "test"), _t_flex)
def flex_10 = Ui.panel(
    Ui.flex(Ui.flex(_t_flex, _t_flex), Ui.button("hello")), Ui.textField()
)
def flex_11 = Ui.panel(
    Ui.flex(Ui.flex(_p_flex, _p_flex), Ui.button("hello")), Ui.textField()
)
def flex_12 = Ui.panel(direction: 'row', _p_flex, _p_flex)
def flex_13 = Ui.panel(_p_flex, _p_flex)
def flex_14 = Ui.flex(alignItems: 'center', justifyContent: 'center',
    Ui.button("hello flex"))
def flex_15 = Ui.panel(alignItems: 'center', justifyContent: 'center',
    Ui.button("hello panel"))
def flex_16 = Ui.panel(Ui.flex(Ui.flex(_t_flex, _t_flex)))
def flex_17 = Ui.panel(
    Ui.flex(direction: 'column', Ui.button("test"), Ui.actionButton("test"))
)
def flex_18 = Ui.panel(
    Ui.form(
        Ui.textField(label: "Name", labelPosition: 'side'),
        Ui.textField(label: "Name", labelPosition: 'side'),
        Ui.textField(label: "Name", labelPosition: 'side'),
        Ui.textField(label: "Name", labelPosition: 'side')
    )
)
def flex_19 = Ui.panel(
    Ui.grid(rows: 'min-content 1fr 1fr', Ui.button("test"), _t_flex, _p_flex)
)
def flex_20 = Ui.panel(
    Ui.tabs(
        Ui.tab(title: 'Tab A', _p_flex),
        Ui.tab(title: 'Tab B', _t_flex)
    )
)
def flex_21 = Ui.panel(
    Ui.button("Test"),
    Ui.tabs(
        Ui.tab(title: 'Tab A', _p_flex),
        Ui.tab(title: 'Tab B', _t_flex)
    ),
    Ui.button("Test"),
    _t_flex
)
def flex_22 = Ui.panel(
    Ui.flex(direction: 'column', Ui.table(_t_flex, margin: '20px'), _t_flex)
)
def flex_23 = Ui.panel(Ui.flex(direction: 'row', Ui.table(_t_flex, margin: '20px'), _t_flex))
def flex_24 = Ui.panel(
    Ui.flex(direction: 'column', Ui.table(_t_flex, height: '200px'), _t_flex)
)

def app = ApplicationContext.get()
[flex_0, flex_1, flex_2, flex_3, flex_4, flex_5, flex_6, flex_7, flex_8, flex_9,
 flex_10, flex_11, flex_12, flex_13, flex_14, flex_15, flex_16, flex_17, flex_18, flex_19,
 flex_20, flex_21, flex_22, flex_23, flex_24].eachWithIndex { v, i ->
    app.setField("flex_${i}", v, "Flex layout test ${i}")
}
