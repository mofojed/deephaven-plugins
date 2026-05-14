// Toast demo: button presses fire transient client-side notifications.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

toastDemo = Ui.component { ->
    Ui.flex(direction: 'column', gap: 'size-100', padding: 'size-200',
        Ui.heading("Toast demo"),
        Ui.text("Each button fires a toast notification."),
        Ui.flex(direction: 'row', gap: 'size-100', wrap: 'wrap',
            Ui.button(variant: 'accent',
                    onPress: { Ui.toast("Plain toast!") },
                    "Show toast"),
            Ui.button(variant: 'primary',
                    onPress: { Ui.toast("Saved successfully", variant: 'positive') },
                    "Positive"),
            Ui.button(variant: 'negative',
                    onPress: { Ui.toast("Something went wrong", variant: 'negative') },
                    "Negative"),
            Ui.button(
                    onPress: { Ui.toast("FYI", variant: 'info', timeout: 3000) },
                    "Info (auto-close)"),
            Ui.button(
                    onPress: { Ui.toast("Did you mean to delete that?",
                            variant: 'neutral',
                            actionLabel: 'Undo',
                            onAction: { Ui.toast("Undone", variant: 'positive') }) },
                    "With Undo action")
        )
    )
}

ApplicationContext.get().setField("toastDemo", toastDemo, "Toast/event channel demo")
