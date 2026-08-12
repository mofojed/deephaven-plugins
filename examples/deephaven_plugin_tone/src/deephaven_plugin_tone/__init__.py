"""
This is a plugin for Deephaven that plays tones in the browser using an
event plugin. Tones are sent from the server with `ui.use_send_event` and
played on the client with the Web Audio API.
"""

from .tone import tone, ToneException
