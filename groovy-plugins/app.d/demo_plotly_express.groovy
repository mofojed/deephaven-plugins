// Tiny plotly-express demo so the combined dev server has a chart to render out of the box.

import io.deephaven.appmode.ApplicationContext
import io.deephaven.engine.util.TableTools
import io.deephaven.plot.express.Express as Dx

def demo_source = TableTools.emptyTable(10).update(
        "X = (int)i",
        "Y = (int)(i * i)",
)

def demo_scatter = Dx.scatter(demo_source, x: "X", y: "Y", title: "plotly-express smoke")
def demo_line = Dx.line(demo_source, x: "X", y: "Y")

def app = ApplicationContext.get()
app.setField("demo_scatter", demo_scatter, "plotly-express scatter demo")
app.setField("demo_line", demo_line, "plotly-express line demo")
