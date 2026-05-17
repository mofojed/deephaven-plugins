// Tiny ui-groovy demo so the combined dev server has a widget to render out of the box.
// Full per-plugin demos live in ui-groovy/run/app.d/ — this file just exposes one to prove
// the plugin loaded.

import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

def demo_counter = Ui.component { ->
    def (count, setCount) = Ui.useState(0)
    Ui.flex(direction: 'column',
            Ui.actionButton("count: ${count}", onPress: { _e -> setCount({ c -> (c as int) + 1 }) }),
            Ui.text("Click the button to verify ui-groovy is wired up."))
}

ApplicationContext.get().setField("demo_counter", demo_counter, "ui-groovy demo widget")
