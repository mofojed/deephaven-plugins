// Groovy port of tests/app.d/ui_table.py. The Python version pulls _stocks from
// deephaven.plot.express.data.stocks() — that helper lives in the Python-only express plugin —
// so we synthesize an equivalent stocks-shaped table here. Column names and types match what
// tests/ui_table.spec.ts expects.

import io.deephaven.api.agg.Aggregation
import io.deephaven.appmode.ApplicationContext
import io.deephaven.engine.util.TableTools
import io.deephaven.ui.Ui

def _t = TableTools.emptyTable(100).update("x = i", "y = Math.sin(i)")

// Hand-roll a stocks-like table so the formatter / databar / heatmap / aggregation cases below
// run without the deephaven.plot.express plugin.
def _stocks = TableTools.emptyTable(100).update(
    "Index = i",
    "Timestamp = '2024-01-01T00:00:00Z' + i * 1_000_000_000L",
    "Sym = (i % 4 == 0) ? `CAT` : (i % 4 == 1) ? `DOG` : (i % 4 == 2) ? `FISH` : `BIRD`",
    "Exchange = (i % 2 == 0) ? `NYSE` : `NASDAQ`",
    "Price = 100.0 + Math.sin(i * 0.1) * 25.0",
    "Dollars = Price * 2",
    "Size = (int) (i * 10) + 1",
    "SPet500 = (double) (i * 1.5)",
    "Random = Math.sin(i * 0.3) * 2"
)

def t_alignment = Ui.table(_t,
    format_: [
        Ui.tableFormat(alignment: 'left'),
        Ui.tableFormat(cols: 'x', alignment: 'center')
    ])

def t_background_color = Ui.table(_t,
    format_: [
        Ui.tableFormat(cols: 'x', if_: 'x > 5', backgroundColor: 'salmon'),
        Ui.tableFormat(cols: 'y', if_: 'y < 0', backgroundColor: 'negative'),
        Ui.tableFormat(cols: 'y', if_: 'y > 0', backgroundColor: 'positive')
    ])

def t_color = Ui.table(_t,
    format_: [
        Ui.tableFormat(backgroundColor: 'subdued-content-bg'),
        Ui.tableFormat(cols: 'x', if_: 'x > 5', color: 'lemonchiffon', backgroundColor: 'salmon'),
        Ui.tableFormat(cols: 'y', if_: 'y < 0', color: 'negative'),
        Ui.tableFormat(cols: 'y', if_: 'y > 0', color: 'positive')
    ])

def t_color_column_source = Ui.table(
    _t.update("bg_color = x % 2 == 0 ? `positive` : `negative`"),
    format_: [
        Ui.tableFormat(cols: 'x', backgroundColor: 'bg_color')
    ],
    hiddenColumns: ['bg_color'])

def t_priority = Ui.table(_t,
    format_: [
        Ui.tableFormat(backgroundColor: 'accent-100'),
        Ui.tableFormat(backgroundColor: 'accent-200', if_: 'x > 0'),
        Ui.tableFormat(backgroundColor: 'accent-300', if_: 'x > 1'),
        Ui.tableFormat(backgroundColor: 'accent-400', if_: 'x > 2'),
        Ui.tableFormat(backgroundColor: 'accent-500', if_: 'x > 3'),
        Ui.tableFormat(backgroundColor: 'accent-600', if_: 'x > 4')
    ])

def t_value_format = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(value: '0.00'),
        Ui.tableFormat(cols: 'Timestamp', value: 'MM/dd/yyyy'),
        Ui.tableFormat(cols: ['Price', 'Dollars'], value: '$0.00')
    ])

def t_display_names = Ui.table(_stocks,
    columnDisplayNames: [Price: 'Price (USD)', Dollars: '$$$'])

def toggle_table_component = { ->
    Ui.component { ->
    def (withFormat, setWithFormat) = Ui.useBoolean(true)
    def (withLower, setWithLower) = Ui.useBoolean(false)
    def (withDatabars, setWithDatabars) = Ui.useBoolean(true)
    def t = Ui.useMemo({ ->
        _stocks.update("SymColor = Sym == `FISH` ? `positive` : `salmon`")
    }, [])

    def databarFormats = [
        Ui.tableFormat(cols: 'Random', mode: Ui.tableDatabar(valuePlacement: 'hide')),
        Ui.tableFormat(cols: 'SPet500',
            mode: Ui.tableDatabar(color: 'info', valuePlacement: 'overlap')),
        Ui.tableFormat(cols: 'Size',
            mode: Ui.tableDatabar(max: 1000, direction: 'RTL',
                color: ['notice', 'positive'])),
        Ui.tableFormat(cols: 'Sym',
            mode: Ui.tableDatabar(valueColumn: 'Price',
                color: ['magenta-200', 'magenta-800']))
    ]

    def styleFormats = [
        Ui.tableFormat(value: '0.00%'),
        Ui.tableFormat(cols: 'Timestamp', value: 'E, dd MMM yyyy HH:mm:ss z'),
        Ui.tableFormat(cols: 'Size', color: 'info', if_: 'Size < 10'),
        Ui.tableFormat(cols: 'Size', color: 'notice', if_: 'Size > 100'),
        Ui.tableFormat(cols: ['Sym', 'Exchange'], alignment: 'center'),
        Ui.tableFormat(cols: ['Sym', 'Exchange'], backgroundColor: 'negative',
            if_: 'Sym=`CAT`'),
        Ui.tableFormat(if_: 'Sym=`DOG`', color: 'oklab(0.6 -0.3 -0.25)'),
        Ui.tableFormat(cols: 'Sym', color: 'SymColor')
    ]

    def formatRules = []
    if (withFormat) formatRules.addAll(styleFormats)
    if (withDatabars) formatRules.addAll(databarFormats)

    def lowerNames = withLower ? t.definition.columnNames.collectEntries { [it, it.toLowerCase()] } : null

    [
        Ui.flex(direction: 'row',
            Ui.button("Turn formatting ${withFormat ? 'off' : 'on'}",
                onPress: { setWithFormat.toggle() }),
            Ui.button("Turn databars ${withDatabars ? 'off' : 'on'}",
                onPress: { setWithDatabars.toggle() }),
            Ui.button(withLower ? "Original case" : "Lowercase",
                onPress: { setWithLower.toggle() })
        ),
        Ui.table(t,
            hiddenColumns: ['SymColor'],
            format_: formatRules ? formatRules : null,
            columnDisplayNames: lowerNames
        )
    ]
    }
}

def toggle_table = toggle_table_component()

def aggs = [
    Ui.tableAgg("count", cols: ['Sym', 'Exchange']),
    Ui.tableAgg("MAX", ignoreCols: 'Timestamp'),
    Ui.tableAgg("Min", cols: 'Random'),
    Ui.tableAgg("FiRsT", ignoreCols: ['Sym', 'Exchange']),
    Ui.tableAgg("Last")
]

def t_bottom_agg = Ui.table(_stocks, aggregations: aggs)

def t_top_agg = Ui.table(_stocks, aggregations: aggs, aggregationsPosition: 'top')

def t_single_agg = Ui.table(_stocks, aggregations: Ui.tableAgg("sum"))

def t_selection_component = { ->
    Ui.component { ->
        def (selection, setSelection) = Ui.useState([])
        def selectionStr = (selection as List).isEmpty()
            ? "None"
            : (selection as List).collect { row ->
                "${row['Sym']?.text}/${row['Exchange']?.text}"
              }.join(', ')

        Ui.flex(direction: 'column',
            Ui.text("Selection: ${selectionStr}"),
            Ui.table(_stocks,
                onSelectionChange: { d -> setSelection(d) },
                alwaysFetchColumns: ['Sym', 'Exchange']
            )
        )
    }
}

def t_selection = t_selection_component()

def t_databar_basic = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Price', mode: Ui.tableDatabar(color: 'positive'))
    ])

def t_databar_multi_cols = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: ['Price', 'Size'],
            mode: Ui.tableDatabar(color: 'info', valuePlacement: 'beside'))
    ])

def t_databar_full_options = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Random',
            mode: Ui.tableDatabar(
                min: -2, max: 2, axis: 'middle', direction: 'LTR',
                valuePlacement: 'beside',
                color: [positive: 'positive', negative: 'negative'],
                opacity: 0.5,
                markers: [[value: 1, color: 'info']]
            )
        )
    ])

def t_databar_conditional = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Size', if_: 'Size > 50',
            mode: Ui.tableDatabar(color: 'positive', max: 1000))
    ])

def t_databar_priority = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Price', mode: Ui.tableDatabar(color: 'info')),
        Ui.tableFormat(cols: 'Price', if_: 'Index > 10',
            mode: Ui.tableDatabar(color: 'positive')),
        Ui.tableFormat(cols: 'Price', if_: 'Index < 5',
            mode: Ui.tableDatabar(color: 'negative'))
    ])

def t_databar_mixed = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Sym', backgroundColor: 'lemonchiffon'),
        Ui.tableFormat(cols: 'Price', mode: Ui.tableDatabar(color: 'info')),
        Ui.tableFormat(cols: 'Size', if_: 'Size > 100', color: 'positive'),
        Ui.tableFormat(cols: 'Size',
            mode: Ui.tableDatabar(color: 'salmon', opacity: 0.5))
    ])

def t_databar_gradient = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Price',
            mode: Ui.tableDatabar(color: ['blue-400', 'purple-800'])),
        Ui.tableFormat(cols: 'Random',
            mode: Ui.tableDatabar(
                color: [
                    positive: ['green-400', 'green-800'],
                    negative: ['red-400', 'red-800']
                ],
                axis: 'middle'
            )
        )
    ])

def t_databar_text_color = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Size', color: 'red',
            mode: Ui.tableDatabar(color: 'purple-800'))
    ])

def t_databar_gradient_text_color = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Size', color: 'orange',
            mode: Ui.tableDatabar(color: ['negative', 'positive']))
    ])

def t_databar_pos_neg_text_color = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Random', color: 'info', mode: Ui.tableDatabar())
    ])

def _heatmap_t = TableTools.emptyTable(20).update("x = i", "neg = i - 10")

def t_heatmap_basic = Ui.table(_heatmap_t,
    format_: [
        Ui.tableFormat(cols: 'x', backgroundColor: Ui.tableHeatmap())
    ])

def t_heatmap_diverging = Ui.table(_heatmap_t,
    format_: [
        Ui.tableFormat(cols: 'neg', backgroundColor: Ui.tableHeatmap(mid: 0))
    ])

def t_heatmap_multistop = Ui.table(_heatmap_t,
    format_: [
        Ui.tableFormat(cols: 'x',
            backgroundColor: Ui.tableHeatmap(
                gradient: ['blue-600', 'cyan-300', 'yellow-300', 'red-600']
            )
        )
    ])

def t_heatmap_positioned_stops = Ui.table(_heatmap_t,
    format_: [
        Ui.tableFormat(cols: 'x',
            backgroundColor: Ui.tableHeatmap(
                gradient: [[0, 'green-600'], [0.2, 'white'], [1, 'red-600']]
            )
        )
    ])

def t_heatmap_text_color = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Random', color: Ui.tableHeatmap(gradient: 'viridis'))
    ])

def t_heatmap_both = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Price',
            color: Ui.tableHeatmap(gradient: ['white', 'black']),
            backgroundColor: Ui.tableHeatmap(gradient: 'inferno')
        )
    ])

def t_heatmap_databar_overlay = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Price',
            backgroundColor: Ui.tableHeatmap(gradient: 'magma'),
            mode: Ui.tableDatabar(color: 'white')
        )
    ])

def t_heatmap_databar_mixed = Ui.table(_stocks,
    format_: [
        Ui.tableFormat(cols: 'Price',
            backgroundColor: Ui.tableHeatmap(gradient: 'cividis')),
        Ui.tableFormat(cols: 'Random',
            mode: Ui.tableDatabar(color: 'info', axis: 'middle'))
    ])

def _rollup_source = TableTools.emptyTable(100).update(
    "Group = (int)(i % 5)", "Subgroup = (int)(i % 3)", "Value = (double) i"
)
def _rollup = _rollup_source.rollup(
    [Aggregation.AggAvg("AvgValue = Value")], "Group", "Subgroup"
)
def t_rollup = Ui.table(_rollup)

def t_rollup_format = Ui.table(_rollup,
    format_: [
        Ui.tableFormat(cols: 'AvgValue', backgroundColor: 'accent-200')
    ])

def _tree_source = TableTools.emptyTable(7).update(
    "ID = (int) i",
    "Parent = i == 0 ? null : (int)((i - 1) / 2)",
    "Value = (int) (i * 10)"
)
def _tree = _tree_source.tree("ID", "Parent")
def t_tree = Ui.table(_tree)

def app = ApplicationContext.get()
[t_alignment: t_alignment, t_background_color: t_background_color, t_color: t_color,
 t_color_column_source: t_color_column_source, t_priority: t_priority,
 t_value_format: t_value_format, t_display_names: t_display_names, toggle_table: toggle_table,
 t_bottom_agg: t_bottom_agg, t_top_agg: t_top_agg, t_single_agg: t_single_agg, t_selection: t_selection,
 t_databar_basic: t_databar_basic, t_databar_multi_cols: t_databar_multi_cols,
 t_databar_full_options: t_databar_full_options, t_databar_conditional: t_databar_conditional,
 t_databar_priority: t_databar_priority, t_databar_mixed: t_databar_mixed,
 t_databar_gradient: t_databar_gradient, t_databar_text_color: t_databar_text_color,
 t_databar_gradient_text_color: t_databar_gradient_text_color,
 t_databar_pos_neg_text_color: t_databar_pos_neg_text_color,
 t_heatmap_basic: t_heatmap_basic, t_heatmap_diverging: t_heatmap_diverging,
 t_heatmap_multistop: t_heatmap_multistop, t_heatmap_positioned_stops: t_heatmap_positioned_stops,
 t_heatmap_text_color: t_heatmap_text_color, t_heatmap_both: t_heatmap_both,
 t_heatmap_databar_overlay: t_heatmap_databar_overlay,
 t_heatmap_databar_mixed: t_heatmap_databar_mixed,
 t_rollup: t_rollup, t_rollup_format: t_rollup_format, t_tree: t_tree
].each { name, value -> app.setField(name, value, "ui.table fixture ${name}") }
