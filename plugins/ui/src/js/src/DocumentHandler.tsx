import React, { useCallback, useMemo, useRef } from 'react';
import shortid from 'shortid';
import { WidgetDescriptor } from '@deephaven/dashboard';
import Log from '@deephaven/log';
import { ReactPanelManagerContext } from './ReactPanelManager';
import { getRootChildren } from './DocumentUtils';
import { EMPTY_ARRAY } from '@deephaven/utils';

const log = Log.module('@deephaven/js-plugin-ui/DocumentHandler');

export type DocumentHandlerProps = React.PropsWithChildren<{
  /** Definition of the widget used to create this document. Used for titling panels if necessary. */
  widget: WidgetDescriptor;

  /** IDs of the panels to use for this document. */
  panelIds?: readonly string[];

  /** Triggered when all panels opened from this document have closed */
  onClose?: () => void;
}>;

/**
 * Handles rendering a document for one widget.
 * The document is a tree of elements. From the root node, the children are either all panels (opening more than one panel),
 * or all non-panels, which will automatically be wrapped in one panel.
 * Responsible for opening any panels or dashboards specified in the document.
 */
function DocumentHandler({
  children,
  widget,
  panelIds = EMPTY_ARRAY,
  onClose,
}: DocumentHandlerProps) {
  log.debug('Rendering document', widget);
  const panelOpenCountRef = useRef(0);
  const panelIdIndex = useRef(0);

  const metadata = useMemo(
    () => ({
      name: widget.name ?? 'Unknown',
      type: widget.type,
    }),
    [widget]
  );

  const handleOpen = useCallback(() => {
    panelOpenCountRef.current += 1;
    log.debug('Panel opened, open count', panelOpenCountRef.current);
  }, []);

  const handleClose = useCallback(() => {
    panelOpenCountRef.current -= 1;
    if (panelOpenCountRef.current < 0) {
      throw new Error('Panel open count is negative');
    }
    log.debug('Panel closed, open count', panelOpenCountRef.current);
    if (panelOpenCountRef.current === 0) {
      onClose?.();
    }
  }, [onClose]);

  const getPanelId = useCallback(() => {
    const panelId = panelIds[panelIdIndex.current] ?? shortid();
    panelIdIndex.current += 1;
    return panelId;
  }, [panelIds]);

  const panelManager = useMemo(
    () => ({
      metadata,
      onOpen: handleOpen,
      onClose: handleClose,
      getPanelId,
    }),
    [metadata, getPanelId, handleClose, handleOpen]
  );

  return (
    <ReactPanelManagerContext.Provider value={panelManager}>
      {getRootChildren(children, widget)}
    </ReactPanelManagerContext.Provider>
  );
}

DocumentHandler.displayName = '@deephaven/js-plugin-ui/DocumentHandler';

export default DocumentHandler;
