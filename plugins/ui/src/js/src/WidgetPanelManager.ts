import { WidgetDescriptor } from '@deephaven/dashboard';
import { useContextOrThrow } from '@deephaven/react-hooks';
import { createContext } from 'react';

/**
 * Manager for panels within a widget. This is used to manage the lifecycle of panels within a widget.
 */
export interface WidgetPanelManager {
  /**
   * Descriptor of the widget. Passed to all panels as metadata.
   * Updating the metadata will cause the panel to be re-opened, or replaced if it is closed.
   * Can also be used for rehydration.
   */
  widget: WidgetDescriptor;

  /** Triggered when a panel is opened */
  onOpen: (panelId: string) => void;

  /** Triggered when a panel is closed */
  onClose: (panelId: string) => void;

  /**
   * Get a panelId from the panel manager.
   * Return a known panel ID if this is a rehydration, otherwise
   * generate a new panel ID if this is a new render or there are no IDs left.
   * */
  getPanelId: () => string;
}

export const WidgetPanelManagerContext =
  createContext<WidgetPanelManager | null>(null);

export function useWidgetPanelManager(): WidgetPanelManager {
  return useContextOrThrow(
    WidgetPanelManagerContext,
    'No WidgetPanelManager found, did you wrap in a WidgetPanelManagerProvider.Context?'
  );
}
