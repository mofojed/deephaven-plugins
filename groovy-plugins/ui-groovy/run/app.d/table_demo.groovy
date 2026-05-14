// Demo: ui.table, itemTableSource (picker fed from a table), and live-data hooks.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.engine.util.TableTools
import io.deephaven.ui.Ui

import java.time.Duration

// A small static table for the table component demo.
fruits = TableTools.newTable(
    TableTools.stringCol("id", "apple", "banana", "cherry", "date"),
    TableTools.stringCol("label", "Apple", "Banana", "Cherry", "Date"),
    TableTools.intCol("price", 1, 2, 3, 4)
)

// A live ticking table for the live-data hooks demo.
ticking = TableTools.timeTable("PT1S").update("X = i", "Y = Math.sin(i * 0.1)").tail(5)

// ── ui.table ──────────────────────────────────────────────────────────────────────────────
tableDemo = Ui.component { ->
    Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
        Ui.heading("ui.table demo"),
        Ui.table(fruits,
            format_: [
                Ui.tableFormat(cols: 'price', value: '0.00'),
                Ui.tableFormat(cols: 'label', backgroundColor: '#e8f5e9'),
            ],
            showSearch: true,
            showQuickFilters: true,
            density: 'compact'
        )
    )
}

// ── itemTableSource feeding a picker ──────────────────────────────────────────────────────
pickerFromTable = Ui.component { ->
    def (selected, setSelected) = Ui.useState("apple")
    Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
        Ui.heading("Picker from a table"),
        Ui.picker(label: 'Fruit', selectedKey: selected, onSelectionChange: { v -> setSelected(v) },
            Ui.itemTableSource(fruits, keyColumn: 'id', labelColumn: 'label')
        ),
        Ui.text("Selected: $selected")
    )
}

// ── live-data hooks (useTableData / useCellData / useRowData / useColumnData) ────────────
liveDemo = Ui.component { ->
    def cell = Ui.useCellData(ticking)
    def row = Ui.useRowData(ticking)
    def rows = Ui.useTableData(ticking)
    def xs = Ui.useColumnData(ticking?.dropColumns("Timestamp", "Y"))

    Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
        Ui.heading("Live-data hooks demo"),
        Ui.text("ticking.head(1) cell: ${cell}"),
        Ui.text("First row: ${row}"),
        Ui.text("Row count in live snapshot: ${rows?.size()}"),
        Ui.text("X column values: ${xs}"),
        Ui.divider(),
        Ui.text("Last 5 rows of the live table:"),
        Ui.table(ticking, density: 'compact', showSearch: false)
    )
}

ApplicationContext.get().setField("tableDemo", tableDemo, "ui.table demo with static fruits table")
ApplicationContext.get().setField("pickerFromTable", pickerFromTable, "Picker fed from an itemTableSource")
ApplicationContext.get().setField("liveDemo", liveDemo, "Live-data hooks against a ticking time table")
