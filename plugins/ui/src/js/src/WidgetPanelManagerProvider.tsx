import React, { useMemo } from 'react';
import {
  WidgetPanelManager,
  WidgetPanelManagerContext,
} from './WidgetPanelManager';

export function WidgetPanelManagerProvider({
  children,
  metadata,
  onOpen,
  onClose,
  getPanelId,
}: React.PropsWithChildren<WidgetPanelManager>): JSX.Element {
  const manager = useMemo(
    () => ({
      metadata,
      onOpen,
      onClose,
      getPanelId,
    }),
    [metadata, onOpen, onClose, getPanelId]
  );
  return (
    <WidgetPanelManagerContext.Provider value={manager}>
      {children}
    </WidgetPanelManagerContext.Provider>
  );
}

export default WidgetPanelManagerProvider;
