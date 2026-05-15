// Groovy port of tests/app.d/ui_grid.py. Same _p_grid substitution as ui_flex.groovy.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.engine.util.TableTools
import io.deephaven.plot.PlottingConvenience
import io.deephaven.ui.Ui

def _t_grid = TableTools.emptyTable(100).update("x = i", "y = Math.sin(i)")
def _p_grid = PlottingConvenience.plot("y", _t_grid, "x", "y").show()

def ui_grid_text_field_input_types_examples = { ->
    Ui.component { ->
        Ui.grid(
            Ui.form(validationBehavior: 'native',
                Ui.textField(label: "Name", type: "text", isRequired: true),
                Ui.textField(label: "Personal Website", type: "url", isRequired: true),
                Ui.textField(label: "Phone", type: "tel", isRequired: true),
                Ui.textField(label: "Email", type: "email", isRequired: true),
                Ui.textField(label: "Password", type: "password", isRequired: true),
                Ui.textField(label: "Search Bar", type: "search")
            ),
            _t_grid
        )
    }
}

def ui_grid_test_component = { ->
    Ui.component { ->
        Ui.grid(Ui.textField(), _t_grid, _t_grid)
    }
}

def grid_0 = ui_grid_test_component()
def grid_1 = ui_grid_text_field_input_types_examples()
def grid_2 = Ui.panel(backgroundColor: 'red', Ui.grid(_t_grid, _t_grid))
def grid_3 = Ui.grid(Ui.button("test"))
def grid_4 = Ui.grid(Ui.textField(label: "test", labelPosition: 'side'))
def grid_5 = Ui.panel(backgroundColor: 'blue',
    Ui.grid(rows: ['auto', 'auto'], _t_grid, _t_grid))
def grid_6 = Ui.panel(backgroundColor: 'green',
    Ui.grid(columns: ['auto', 'auto'], _t_grid, _t_grid))
def grid_7 = Ui.panel(Ui.grid(columns: ['auto', 'auto'], _t_grid, _p_grid))
def grid_8 = Ui.panel(Ui.grid(_t_grid, _p_grid))
def grid_9 = Ui.panel(Ui.grid(Ui.textField(label: "test"), _t_grid))
def grid_10 = Ui.panel(
    Ui.grid(Ui.flex(_t_grid, _t_grid), Ui.button("hello")), Ui.textField()
)
def grid_11 = Ui.panel(
    Ui.grid(Ui.flex(_p_grid, _p_grid), Ui.button("hello")), Ui.textField()
)
def grid_12 = Ui.panel(Ui.grid(columns: ['auto', 'auto'], _p_grid, _p_grid))
def grid_13 = Ui.panel(Ui.grid(_p_grid, _p_grid))
def grid_14 = Ui.grid(alignItems: 'center', justifyContent: 'center',
    Ui.button("hello flex"))
def grid_15 = Ui.panel(
    Ui.grid(alignItems: 'center', justifyContent: 'center', Ui.button("hello panel"))
)
def grid_16 = Ui.panel(Ui.grid(Ui.grid(_t_grid, _t_grid)))
def grid_17 = Ui.panel(
    Ui.grid(rows: ['auto', 'auto'], Ui.button("test"), Ui.actionButton("test"))
)
def grid_18 = Ui.panel(
    Ui.grid(
        Ui.form(
            Ui.textField(label: "Name", labelPosition: 'side'),
            Ui.textField(label: "Name", labelPosition: 'side'),
            Ui.textField(label: "Name", labelPosition: 'side'),
            Ui.textField(label: "Name", labelPosition: 'side')
        )
    )
)
def grid_19 = Ui.panel(
    Ui.grid(rows: 'min-content 1fr 1fr', Ui.button("test"), _t_grid, _p_grid)
)
def grid_20 = Ui.panel(
    Ui.grid(
        Ui.tabs(
            Ui.tab(title: 'Tab A', _p_grid),
            Ui.tab(title: 'Tab B', _t_grid)
        )
    )
)
def grid_21 = Ui.panel(
    Ui.grid(
        Ui.button("Test"),
        Ui.tabs(
            Ui.tab(title: 'Tab A', _p_grid),
            Ui.tab(title: 'Tab B', _t_grid)
        ),
        Ui.button("Test"),
        _t_grid
    )
)
def grid_22 = Ui.panel(
    Ui.grid(rows: ['auto', 'auto'], Ui.table(_t_grid, margin: '20px'), _t_grid)
)
def grid_23 = Ui.panel(
    Ui.grid(columns: ['auto', 'auto'], Ui.table(_t_grid, margin: '20px'), _t_grid)
)
def grid_24 = Ui.panel(
    Ui.grid(rows: ['auto', 'auto'], Ui.table(_t_grid, height: '200px'), _t_grid)
)

def app = ApplicationContext.get()
[grid_0, grid_1, grid_2, grid_3, grid_4, grid_5, grid_6, grid_7, grid_8, grid_9,
 grid_10, grid_11, grid_12, grid_13, grid_14, grid_15, grid_16, grid_17, grid_18, grid_19,
 grid_20, grid_21, grid_22, grid_23, grid_24].eachWithIndex { v, i ->
    app.setField("grid_${i}", v, "Grid layout test ${i}")
}
