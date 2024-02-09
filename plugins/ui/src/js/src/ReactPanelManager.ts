import { useContextOrThrow } from '@deephaven/react-hooks';
import { createContext } from 'react';

export type ReactPanelManager = {
  /**
   * Metadata to pass to all the panels.
   * Updating the metadata will cause the panel to be re-opened, or replaced if it is closed.
   * Can also be used for rehydration.
   */
  metadata: Record<string, unknown>;

  /** Triggered when a panel is opened */
  onOpen: (panelId: string) => void;

  /** Triggered when a panel is closed */
  onClose: (panelId: string) => void;

  /**
   * Get a panelId from the panel manager.
   * Return a known panel ID if this is a rehydration, otherwise
   * generate a new panelId if this is a new render or there are no IDs left.
   * */
  getPanelId: () => string;
};

export const ReactPanelManagerContext = createContext<ReactPanelManager | null>(
  null
);

export function useReactPanelManager(): ReactPanelManager {
  return useContextOrThrow(
    ReactPanelManagerContext,
    'No ReactPanelManager found, did you wrap in a ReactPanelManagerProvider.Context?'
  );
}
