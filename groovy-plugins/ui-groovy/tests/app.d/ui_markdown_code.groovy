// Groovy port of tests/app.d/ui_markdown_code.py.
import io.deephaven.appmode.ApplicationContext
import io.deephaven.ui.Ui

def markdown_code = Ui.markdown('''
This code block `print("Hello world")` should be in-line.

Here\'s a multi-line code block:
```
print("Hello there")
```
''')

ApplicationContext.get().setField("markdown_code", markdown_code, "Markdown code block test")
