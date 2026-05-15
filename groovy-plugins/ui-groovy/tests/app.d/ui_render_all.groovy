// Groovy port of tests/app.d/ui_render_all.py — smoke-renders every supported component so the
// JS plugin's encoder visits every name. Same exported variable names as the Python version so
// tests/ui.spec.ts ('UI all components render N') can target this fixture unchanged.

import io.deephaven.appmode.ApplicationContext
import io.deephaven.engine.util.TableTools
import io.deephaven.ui.Html
import io.deephaven.ui.Ui

def iconNames = ["vsAccount"]
def columns = [
    "Id = (int) i",
    "Display = `Display ` + i",
    "Description = `Description ` + i",
    "Icon = `vsAccount`"
]
def _column_types = TableTools.emptyTable(20).update(columns as String[])

def _item_table_source_with_icons = Ui.itemTableSource(_column_types,
    keyColumn: "Id", labelColumn: "Display", iconColumn: "Icon")

def _item_table_source_with_action_group = Ui.itemTableSource(_column_types,
    keyColumn: "Id", labelColumn: "Display", iconColumn: "Icon",
    actions: Ui.listActionGroup(
        Ui.item("Edit"),
        Ui.item("Delete")
    )
)

def _item_table_source_with_action_menu = Ui.itemTableSource(_column_types,
    keyColumn: "Id", labelColumn: "Display", iconColumn: "Icon",
    actions: Ui.listActionMenu(
        Ui.item("Edit"),
        Ui.item("Delete")
    )
)

def ui_components1 = { ->
    Ui.component { ->
        [
            Ui.actionButton("Action Button"),
            Ui.actionGroup("Aaa", "Bbb", "Ccc"),
            Ui.actionMenu("Aaa", "Bbb", "Ccc"),
            Ui.badge(variant: 'positive', "Licensed"),
            Ui.buttonGroup(Ui.button("One"), Ui.button("Two")),
            Ui.button("Button"),
            Ui.breadcrumbs(
                Ui.item("Deephaven", key: 'deephaven'),
                Ui.item("Products", key: 'products'),
                Ui.item("Community Core", key: 'community_core')
            ),
            Ui.calendar(value: "2021-01-01"),
            Ui.checkbox("Checkbox"),
            Ui.column("Column child A", "Column child B", "Column child C"),
            Ui.content("Content"),
            Ui.contextualHelp("Contextual Help", "Content"),
            Ui.datePicker(label: "Date Picker", value: "2021-01-01"),
            Ui.dateRangePicker(label: "Date Range Picker",
                value: [start: "2021-01-01", end: "2021-01-02"]),
            Ui.flex("Content before", Ui.divider(orientation: 'vertical'), "Content after"),
            Ui.flex("Flex default child A", "Flex default child B"),
            Ui.flex(direction: 'column', "Flex column child A", "Flex column child B"),
            Ui.form("Form")
        ]
    }
}

def ui_components2 = { ->
    Ui.component { ->
        [
            Ui.footer("© All rights reserved."),
            Ui.fragment("Fragment"),
            Ui.grid("Grid A", "Grid B"),
            Ui.heading("Heading"),
            Ui.icon("vsSymbolMisc"),
            Ui.illustratedMessage(
                Ui.icon("vsWarning"),
                Ui.heading("Warning"),
                Ui.content("This is a warning message.")
            ),
            Ui.inlineAlert(variant: 'positive',
                Ui.heading("Purchase completed"),
                Ui.content("You'll get a confirmation email with your order details shortly.")
            ),
            Ui.labeledValue(label: "File name", value: "Budget.xls"),
            Ui.link(href: "https://deephaven.io/", "Learn more about Deephaven"),
            Ui.listView(_item_table_source_with_action_group,
                ariaLabel: "List View - List action group", minHeight: 'size-1600'),
            Ui.listView(_item_table_source_with_action_menu,
                ariaLabel: "List View - List action menu", minHeight: 'size-1600'),
            Ui.numberField("Number Field", ariaLabel: "Number field"),
            Ui.picker(ariaLabel: "Picker with Section",
                "Aaa",
                "Bbb",
                Ui.section(title: 'Section A', "Ccc", "Ddd")),
            Ui.picker(_item_table_source_with_icons, ariaLabel: "Picker", defaultSelectedKey: 15),
            Ui.radioGroup(label: "Radio Group", orientation: 'HORIZONTAL',
                Ui.radio(value: 'one', "One"),
                Ui.radio(value: 'two', "Two")
            ),
            Ui.rangeCalendar(defaultValue: [start: "2021-01-01", end: "2021-01-02"])
        ]
    }
}

def ui_components3 = { ->
    Ui.component { ->
        [
            Ui.rangeSlider(label: "Range Slider", defaultValue: [start: 10, end: 99]),
            Ui.row("Row child A", "Row child B"),
            Ui.slider(label: "Slider", defaultValue: 40, minValue: -100.0, maxValue: 100.0, step: 0.1),
            Ui.switch_("Switch"),
            Ui.tagGroup(
                Ui.item("Tag 1", key: '1'),
                Ui.item("Tag 2", key: '2'),
                Ui.item("Tag 3", key: '3')
            ),
            Ui.text("Text"),
            Ui.textField(label: "Text Field", defaultValue: "Text Field",
                Ui.icon("vsSymbolMisc")),
            Ui.timeField(defaultValue: "12:30:00", hourCycle: 24),
            Ui.toggleButton(Ui.icon("vsBell"), "By Exchange"),
            Ui.view("View"),
            // Place last so its popover doesn't overlap other components.
            Ui.menuTrigger(defaultOpen: true,
                Ui.actionButton("Menu"),
                Ui.menu(
                    Ui.item("Menu Item 1"),
                    Ui.item("Menu Item 2")
                )
            ),
            Ui.disclosure(title: "Heading", panel: "Content")
        ]
    }
}

def ui_html_elements = { ->
    Ui.component { ->
        Html.div("div")
    }
}

def _my_components1 = ui_components1()
def _my_components2 = ui_components2()
def _my_components3 = ui_components3()
def _my_html_elements = ui_html_elements()

def ui_render_all1 = Ui.dashboard(
    Ui.stack(
        Ui.panel(title: 'Panel B',
            Ui.table(_column_types),
            Ui.grid(columns: ['1fr', '1fr', '1fr'], width: '100%',
                _my_components1,
                _my_html_elements
            )
        )
    )
)

def ui_render_all2 = Ui.dashboard(
    Ui.stack(
        Ui.panel(title: 'Panel C',
            Ui.grid(columns: ['1fr', '1fr', '1fr'], width: '100%',
                _my_components2
            )
        )
    )
)

def ui_render_all3 = Ui.dashboard(
    Ui.stack(
        Ui.panel(title: 'Panel D',
            Ui.grid(columns: ['1fr', '1fr', '1fr'], width: '100%',
                _my_components3
            )
        )
    )
)

def app = ApplicationContext.get()
app.setField("ui_render_all1", ui_render_all1, "All components render 1")
app.setField("ui_render_all2", ui_render_all2, "All components render 2")
app.setField("ui_render_all3", ui_render_all3, "All components render 3")
