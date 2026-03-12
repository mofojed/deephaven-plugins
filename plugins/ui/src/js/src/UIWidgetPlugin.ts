import { type WidgetPlugin, PluginType } from '@deephaven/plugin';
import { vsGraph } from '@deephaven/icons';
import type { dh } from '@deephaven/jsapi-types';
import { DASHBOARD_ELEMENT, WIDGET_ELEMENT } from './widget/WidgetUtils';
import UIComponent from './UIComponent';
import styles from './styles.scss?inline';

// We need to inject the styles into the document when we're loaded... we only want to do this once.
const styleElement = document.createElement('style');
styleElement.textContent = styles;
document.head.appendChild(styleElement);

export const UIWidgetPlugin: WidgetPlugin<dh.Widget> = {
  name: '@deephaven/js-plugin-ui',
  type: PluginType.WIDGET_PLUGIN,
  supportedTypes: [WIDGET_ELEMENT, DASHBOARD_ELEMENT],
  component: UIComponent,
  icon: vsGraph,
};

export default UIWidgetPlugin;
