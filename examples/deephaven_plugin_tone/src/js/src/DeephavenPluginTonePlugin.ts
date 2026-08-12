import { type ElementPlugin, PluginType } from '@deephaven/plugin';
import playTone, { TONE_EVENT, type ToneParams } from './Tone';

// An element plugin can optionally handle events sent from the server via
// `use_send_event`. This is the same mechanism used by built-in deephaven.ui
// events, which are namespaced with `deephaven.ui` (e.g. `deephaven.ui.toast`).
// Plugins should namespace their own events the same way, using their package
// namespace as the prefix. Adding an `eventMapping` is
// optional - leave it as an empty map if your plugin does not handle events.
// Note that on the server side, `use_send_event` must be called from the render
// thread (see https://deephaven.io/core/ui/docs/hooks/use_render_queue/).
type ElementPluginWithEvents = ElementPlugin & {
  eventMapping: Record<string, (params: Record<string, unknown>) => void>;
};

// Register the plugin with Deephaven
export const DeephavenPluginTonePlugin: ElementPluginWithEvents = {
  // The name of the plugin
  name: 'deephaven-plugin-tone',
  // The type of plugin - this will generally be ELEMENT_PLUGIN
  type: PluginType.ELEMENT_PLUGIN,
  // This plugin does not provide any elements, only an event handler, so the
  // element mapping is empty.
  mapping: {},
  // Map event names to handlers to react to events sent from the server via
  // `use_send_event`. The event name must match the name passed to `use_send_event`
  // by the `tone` function in tone.py.
  eventMapping: {
    [TONE_EVENT]: (params: Record<string, unknown>) => {
      playTone(params as ToneParams);
    },
  },
};

export default DeephavenPluginTonePlugin;
