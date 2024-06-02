import { type WidgetPlugin, PluginType } from '@deephaven/plugin';
import { vsGraph } from '@deephaven/icons';
import { DeephavenUiExperimentalView } from './DeephavenUiExperimentalView';

// Register the plugin with Deephaven
export const DeephavenUiExperimentalPlugin: WidgetPlugin = {
  // The name of the plugin
  name: 'deephaven-ui-experimental',
  // The type of plugin - this will generally be WIDGET_PLUGIN
  type: PluginType.WIDGET_PLUGIN,
  // The supported types for the plugin. This should match the value returned by `name`
  // in DeephavenUiExperimentalType in deephaven_ui_experimental_type.py
  supportedTypes: 'DeephavenUiExperimental',
  // The component to render for the plugin
  component: DeephavenUiExperimentalView,
  // The icon to display for the plugin
  icon: vsGraph,
};

export default DeephavenUiExperimentalPlugin;
