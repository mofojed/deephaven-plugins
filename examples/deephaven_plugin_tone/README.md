# deephaven_plugin_tone

This is a Python plugin for Deephaven generated from a [deephaven-plugin](https://github.com/deephaven/deephaven-plugins) template.

Specifically, this plugin is an element plugin that extends `deephaven.ui` with a `tone` function that plays short sounds to the user using the browser's [Web Audio API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API). The sound is synthesized natively in the browser from an oscillator, so no audio file needs to be transferred. Use tones to provide quick audio feedback, such as signaling that a long-running task has finished or that new data has arrived.

This plugin demonstrates the `eventMapping` functionality of element plugins: the Python side sends an event to the client with `ui.use_send_event`, and the JavaScript side registers a handler for that event which plays the sound. It does not provide any elements of its own.

## Plugin Structure

The `src` directory contains the Python and JavaScript code for the plugin.  
Within the `src` directory, the deephaven_plugin_tone directory contains the Python code, and the `js` directory contains the JavaScript code.

The Python files have the following structure:  
`tone.py` defines the `tone` function, which validates and normalizes the requested notes and sends a `deephaven_plugin_tone.event` event to the client using `use_send_event`.  
`register.py` registers the plugin with Deephaven. This file will not need to be modified for most plugins at the initial stages, but will need to be if the package is renamed or JavaScript files are moved.

The JavaScript files have the following structure:  
`DeephavenPluginTonePlugin.ts` registers the plugin with Deephaven. Since this plugin does not provide any elements, its element `mapping` is empty. Its `eventMapping` maps the `deephaven_plugin_tone.event` event name to a handler that plays the requested tones (the same mechanism used by built-in `deephaven.ui` events, which are namespaced with `deephaven.ui`, such as `deephaven.ui.toast`). Namespace your plugin's event names with your package namespace to avoid collisions.  
`Tone.ts` implements the event handler: it converts note names to frequencies and schedules oscillators with the Web Audio API.

Like `ui.toast`, `use_send_event` must be called from the render thread, either while a `@ui.component` is rendering or from an event handler it triggers. Calling it from a background thread, such as a table listener, raises an error. To send an event from off the render thread, queue it with the [`use_render_queue` hook](https://deephaven.io/core/ui/docs/hooks/use_render_queue/).

## Using plugin_builder.py

The `plugin_builder.py` script is the recommended way to build the plugin.
See [Building the Plugin](#building-the-plugin) for more information if you want to build the plugin manually instead.

To use `plugin_builder.py`, first set up your Python environment and install the required packages.  
To build the plugin, you will need `npm` and `python` installed, as well as the `build` package for Python.
`nvm` is also strongly recommended, and an `.nvmrc` file is included in the project.
The script uses `watchdog` and `deephaven-server` for `--watch` mode and `--server` mode, respectively.

```sh
cd deephaven_plugin_tone
python -m venv .venv
source .venv/bin/activate
cd src/js
nvm install
npm install
cd ../..
pip install --upgrade -r requirements.txt
pip install deephaven-server watchdog
```

First, run an initial install of the plugin:
This builds and installs the full plugin, including the JavaScript code.

```sh
python plugin_builder.py --install --js
```

After this, more advanced options can be used.
For example, if only iterating on the plugins with no version bumps, use the `--reinstall` flag for faster builds.
This adds `--force-reinstall --no-deps` to the `pip install` command.

```sh
python plugin_builder.py --reinstall --js
```

If only the Python code has changed, the `--js` flag can be omitted.

```sh
python plugin_builder.py --reinstall
```

Additional especially useful flags are `--watch` and `--server`.
`--watch` will watch the Python and JavaScript files for changes and rebuild the plugin when they are modified.
`--server` will start the Deephaven server with the plugin installed.
Taken in combination with `--reinstall` and `--js`, this command will rebuild and restart the server when changes are made to the plugin.

```sh
python plugin_builder.py --reinstall --js --watch --server
```

If interested in passing args to the server, the `--server-arg` flag can be used as well
Check `deephaven server --help` for more information on the available arguments.

```sh
python plugin_builder.py --reinstall --js --watch --server --server-arg --port=9999
```

See [Using the Plugin](#using-the-plugin) for more information on how to use the plugin.

## Manually Building the Plugin

To build the plugin, you will need `npm` and `python` installed, as well as the `build` package for Python.
`nvm` is also strongly recommended, and an `.nvmrc` file is included in the project.
The python venv can be created and the recommended packages installed with the following commands:

```sh
cd deephaven_plugin_tone
python -m venv .venv
source .venv/bin/activate
pip install --upgrade -r requirements.txt
```

Build the JavaScript plugin from the `src/js` directory:

```sh
cd src/js
nvm install
npm install
npm run build
```

Then, build the Python plugin from the top-level directory:

```sh
cd ../..
python -m build --wheel
```

The built wheel file will be located in the `dist` directory.

If you modify the JavaScript code, remove the `build` and `dist` directories before rebuilding the wheel:

```sh
rm -rf build dist
```

## Installing the Plugin

The plugin can be installed into a Deephaven instance with `pip install <wheel file>`.
The wheel file is stored in the `dist` directory after building the plugin.
Exactly how this is done will depend on how you are running Deephaven.
If using the venv created above, the plugin and server can be created with the following commands:

```sh
pip install deephaven-server
pip install dist/deephaven_plugin_tone-0.0.1.dev0-py3-none-any.whl
deephaven server
```

See the [plug-in documentation](https://deephaven.io/core/docs/how-to-guides/use-plugins/) for more information.

## Using the Plugin

Once the Deephaven server is running, the plugin should be available to use.

```python
from deephaven import ui
from deephaven_plugin_tone import tone

btn = ui.button(
    "Play tone",
    on_press=lambda: tone("C5"),
    variant="primary",
)
```

### Notes

Tones are triggered using the `tone` function. A note can be specified either by name (such as `"C4"`, `"F#3"`, or `"Bb5"`) or as a frequency in Hertz (such as `440`). Note names use scientific pitch notation, where `A4` is 440 Hz.

```python
from deephaven import ui
from deephaven_plugin_tone import tone


@ui.component
def note_buttons():
    return ui.button_group(
        ui.button("Note name", on_press=lambda: tone("A4")),
        ui.button("Frequency", on_press=lambda: tone(440)),
    )


my_note_buttons = note_buttons()
```

### Rests

Insert a pause into a sequence with `None`. A rest produces no sound but still
takes up its `duration`, so you can control the spacing between phrases
independently of the uniform `gap`. Give a rest its own length with a
`(None, duration)` tuple.

```python
from deephaven import ui
from deephaven_plugin_tone import tone

btn = ui.button(
    "Play with a pause",
    on_press=lambda: tone(
        ["C5", (None, 0.4), "C5"],
        duration=0.15,
    ),
    variant="primary",
)
```

### Sequences

To play a melody, pass a list of notes. Each note plays in turn, separated by a short `gap`. By default every note uses the same `duration`, but you can give a note its own duration by passing a `(note, duration)` tuple. Durations and gaps are measured in seconds.

```python
from deephaven import ui
from deephaven_plugin_tone import tone

btn = ui.button(
    "Play scale",
    on_press=lambda: tone(
        ["C4", "D4", "E4", "F4", "G4", "A4", "B4", ("C5", 0.5)],
        duration=0.15,
    ),
    variant="primary",
)
```

### Chords

To play notes simultaneously, group them in a nested list. Each nested list is a chord whose notes sound together. You can mix single notes and chords in the same sequence, and a chord can be given its own duration with a `(chord, duration)` tuple.

```python
from deephaven import ui
from deephaven_plugin_tone import tone

btn = ui.button(
    "Play chords",
    on_press=lambda: tone(
        [
            ["C4", "E4", "G4"],
            ["F4", "A4", "C5"],
            (["G4", "B4", "D5"], 0.6),
        ],
        duration=0.4,
    ),
    variant="primary",
)
```

### Waveform and volume

The `waveform` option selects the oscillator shape: `"sine"` (the default), `"square"`, `"triangle"`, or `"sawtooth"`. The `gain` option sets the volume from `0` (silent) to `1` (loudest).

```python
from deephaven import ui
from deephaven_plugin_tone import tone

btn = ui.button(
    "Play buzzer",
    on_press=lambda: tone("A3", waveform="sawtooth", gain=0.3),
    variant="primary",
)
```

### Playing a jingle

Combining chords, rests, and per-note durations lets you play a short jingle.
This example recreates the Deephaven outro sting: a single strum of an E major
chord, a pause, and then the same chord strummed several times to finish.

```python
from deephaven import ui
from deephaven_plugin_tone import tone

_CHORD = ["E4", "G#4", "B4", "E5"]

btn = ui.button(
    "Play jingle",
    on_press=lambda: tone(
        [
            (_CHORD, 0.35),
            (None, 0.55),
            (_CHORD, 0.12),
            (_CHORD, 0.12),
            (_CHORD, 0.12),
            (_CHORD, 0.12),
            (_CHORD, 0.3),
        ],
        gap=0.06,
        waveform="triangle",
        gain=0.6,
    ),
    variant="primary",
)
```

### Autoplay restrictions

Browsers block audio until the user has interacted with the page. Playing a tone in response to a user action, such as pressing a button, works reliably. A tone triggered without a prior interaction, such as from a ticking table before the user has clicked anything, may not be audible until the user interacts with the page.

### Tone from table example

This example plays a tone from the latest update of a ticking table. Note that the tone must be triggered on the render thread, whereas the table listener may be fired from another thread. Therefore you must use the render queue to trigger the tone.

```python
from deephaven import time_table
from deephaven import ui
from deephaven_plugin_tone import tone

_source = time_table("PT2S").update("X = i").tail(5)


@ui.component
def tone_table(t):
    render_queue = ui.use_render_queue()

    def listener_function(update, is_replay):
        render_queue(lambda: tone("C5"))

    ui.use_table_listener(t, listener_function, [])
    return t


my_tone_table = tone_table(_source)
```

## Running the Tests

The Python tests live in the `tests` directory and require `deephaven-server` to be installed. Run them from the plugin root so the JVM is initialized by `tests/__init__.py`:

```sh
PYTHONPATH=src python -m unittest
```

The JavaScript tests live alongside the source in `src/js/src` and run with Jest:

```sh
cd src/js
npm test
```

## Debugging the Plugin

It's recommended to run through all the steps in [Using plugin_builder.py](#Using-plugin_builder.py) and [Using the Plugin](#Using-the-plugin) to ensure the plugin is working correctly.  
Then, make changes to the plugin and rebuild it to see the changes in action.
Checkout the [Deephaven plugins repo](https://github.com/deephaven/deephaven-plugins), which is where this template was generated from, for more examples and information.  
The `plugins` folder contains current plugins that are developed and maintained by Deephaven.  
Below are some common issues and how to resolve them as you develop your plugin.  
If there is an issue with the process while following the Installation and Usage steps on the originally generated plugin, please open an issue.

### The Plugin is Not Working

#### Checking if the Plugin is Registered

If the tone does not play or an error is thrown that the import is not found, the plugin may not be registered correctly.
To verify the plugin is registered, check either the console logs or the versions in the settings panel.

- In the console logs, there should be a messaging saying `Plugins loaded:` with a map that includes this plugin.  
  ![plugin map](./_assets/plugin_map.png 'Plugin Map')

- To get to the settings panel, click on the gear icon in the top right corner of the Deephaven window. Towards the bottom this plugin should be listed.  
  ![plugin settings](./_assets/plugin_settings.png 'Plugin Settings')
- If the plugin is not listed, attempt to rebuild and reinstall the plugin and check for errors during that process.

#### Checking if the Python Package is Installed

- Running `pip list` in the `.venv` environment should show the Python package installed, but this is not a guarantee that the plugin is registered properly.
- The version can also be checked directly from the Python console with:

```{python}
from importlib.metadata import version
print(version("deephaven_plugin_tone"))
```

### The Plugin is Registered but Not Functioning Correctly

Check both the Python and JavaScript logs for errors as either side could be causing the issue.

## Distributing the Plugin

To distribute the plugin, you can upload the wheel file to a package repository, such as [PyPI](https://pypi.org/).
The version of the plugin can be updated in the `setup.cfg` file.

There is a separate instance of PyPI for testing purposes.
Start by creating an account at [TestPyPI](https://test.pypi.org/account/register/).
Then, get an API token from [account management](https://test.pypi.org/manage/account/#api-tokens), setting the “Scope” to “Entire account”.

To upload to the test instance, use the following commands:

```sh
python -m pip install --upgrade twine
python -m twine upload --repository testpypi dist/*
```

Now, you can install the plugin from the test instance. The extra index is needed to find dependencies:

```sh
pip install --index-url https://test.pypi.org/simple/ --extra-index-url https://pypi.org/simple/ deephaven_plugin_tone
```

For a production release, create an account at [PyPI](https://pypi.org/account/register/).
Then, get an API token from [account management](https://pypi.org/manage/account/#api-tokens), setting the “Scope” to “Entire account”.

To upload to the production instance, use the following commands.
Note that `--repository` is the production instance by default, so it can be omitted:

```sh
python -m pip install --upgrade twine
python -m twine upload dist/*
```

Now, you can install the plugin from the production instance:

```sh
pip install deephaven_plugin_tone
```

See the [Python packaging documentation](https://packaging.python.org/en/latest/tutorials/packaging-projects/#uploading-the-distribution-archives) for more information.
