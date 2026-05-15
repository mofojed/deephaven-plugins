// Groovy port of tests/app.d/ui_dialog.py.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

def my_modal = Ui.panel(
    Ui.dialogTrigger(isDismissable: true, type: 'modal', defaultOpen: true,
        Ui.actionButton("Trigger Modal"),
        Ui.dialog(
            Ui.heading("Modal"),
            Ui.content("This is a modal.")
        )
    )
)

def my_popover = Ui.panel(
    Ui.dialogTrigger(type: 'popover', defaultOpen: true,
        Ui.actionButton("Trigger Popover"),
        Ui.dialog(
            Ui.heading("Popover"),
            Ui.content("This is a popover.")
        )
    )
)

def my_tray = Ui.panel(
    Ui.dialogTrigger(type: 'tray', defaultOpen: true,
        Ui.actionButton("Trigger Tray"),
        Ui.dialog(
            Ui.heading("Tray"),
            Ui.content("This is a tray.")
        )
    )
)

def my_fullscreen = Ui.panel(
    Ui.dialogTrigger(type: 'fullscreen', defaultOpen: true,
        Ui.actionButton("Trigger Fullscreen"),
        Ui.dialog(
            Ui.heading("Fullscreen"),
            Ui.content(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin sit            amet tristique risus. In sit amet suscipit lorem. Orci varius            natoque penatibus et magnis dis parturient montes, nascetur            ridiculus mus. In condimentum imperdiet metus non condimentum. Duis            eu velit et quam accumsan tempus at id velit. Duis elementum            elementum purus, id tempus mauris posuere a. Nunc vestibulum sapien            pellentesque lectus commodo ornare."
            )
        )
    )
)

def my_fullscreen_takeover = Ui.panel(
    Ui.dialogTrigger(type: 'fullscreenTakeover', defaultOpen: true,
        Ui.actionButton("Trigger Fullscreen"),
        Ui.dialog(
            Ui.heading("Fullscreen"),
            Ui.content(
                Ui.form(
                    Ui.textField(label: "Name"),
                    Ui.textField(label: "Email address"),
                    Ui.checkbox("Make profile private")
                )
            )
        )
    )
)

def app = ApplicationContext.get()
app.setField("my_modal", my_modal, "Modal dialog test")
app.setField("my_popover", my_popover, "Popover dialog test")
app.setField("my_tray", my_tray, "Tray dialog test")
app.setField("my_fullscreen", my_fullscreen, "Fullscreen dialog test")
app.setField("my_fullscreen_takeover", my_fullscreen_takeover, "Fullscreen takeover test")
