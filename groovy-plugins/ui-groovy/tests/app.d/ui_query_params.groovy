// Groovy port of tests/app.d/ui_query_params.py.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

def ui_query_params_component = { ->
    Ui.component { ->
        Map<String, List<String>> params = Ui.useQueryParams()

        if (!params || params.isEmpty()) {
            return Ui.panel(title: 'Query Params', Ui.text("No query params"))
        }

        def items = []
        params.each { key, values ->
            (values as List).each { value ->
                items << Ui.text("${key}=${value}", key: "${key}-${value}")
            }
        }

        Ui.panel(title: 'Query Params',
            Ui.flex(direction: 'column', *(items as Object[]))
        )
    }
}

def ui_query_param_single_component = { ->
    Ui.component { ->
        def pageVal = Ui.useQueryParam("page")
        Ui.panel(title: 'Query Param Single',
            Ui.text(pageVal != null ? "page=${pageVal}" : "page=None")
        )
    }
}

def ui_set_query_param_component = { ->
    Ui.component { ->
        def pageVal = Ui.useQueryParam("counter")
        def setCounter = Ui.useSetQueryParam("counter")
        int current = pageVal != null ? Integer.parseInt(pageVal as String) : 0

        Ui.panel(title: 'Set Query Param',
            Ui.flex(direction: 'column',
                Ui.text("counter=${current}"),
                Ui.actionButton("Increment (current: ${current})",
                    onPress: { setCounter.call(String.valueOf(current + 1)) })
            )
        )
    }
}

def ui_query_params = ui_query_params_component()
def ui_query_param_single = ui_query_param_single_component()
def ui_set_query_param = ui_set_query_param_component()

def app = ApplicationContext.get()
app.setField("ui_query_params", ui_query_params, "All-params reader")
app.setField("ui_query_param_single", ui_query_param_single, "Single-param reader")
app.setField("ui_set_query_param", ui_set_query_param, "Query-param writer")
