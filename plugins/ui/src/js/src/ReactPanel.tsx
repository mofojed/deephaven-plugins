import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import ReactDOM from 'react-dom';
import {
  LayoutUtils,
  PanelEvent,
  useLayoutManager,
  useListener,
} from '@deephaven/dashboard';
import Log from '@deephaven/log';
import PortalPanel from './PortalPanel';
import { useWidgetPanelManager } from './WidgetPanelManager';
import { ReactPanelProps } from './layout/LayoutUtils';
import { useParentItem } from './layout/ParentItemContext';
import { ReactPanelContext } from './ReactPanelContext';
import ScopedIdWrapper from './elements/ScopedIdWrapper';
import { usePortalPanelManager } from './PortalPanelManagerContext';

const log = Log.module('@deephaven/js-plugin-ui/ReactPanel');

/**
 * Adds and tracks a panel to the GoldenLayout. When the child element is updated, the contents of the panel will also be updated. When unmounted, the panel will be removed.
 */
function ReactPanel({ children, title }: ReactPanelProps) {
  const layoutManager = useLayoutManager();
  const panelManager = useWidgetPanelManager();
  const { widget, onClose, onOpen, getPanelId } = panelManager;
  const panelId = useMemo(() => getPanelId(), [getPanelId]);
  const portalManager = usePortalPanelManager();
  const element = portalManager.get(panelId);

  // If there is already a portal that exists, then we're rehydrating from a dehydrated state
  // Initialize the `isPanelOpenRef` and `openedWidgetRef` accordingly on initialization
  const isPanelOpenRef = useRef(element != null);
  const openedWidgetRef = useRef<Record<string, unknown>>(
    element != null ? widget : null
  );
  const parent = useParentItem();
  const { eventHub } = layoutManager;

  console.log('XXX ReactPanel portalManager', portalManager);
  log.debug2('Rendering panel', panelId);

  useEffect(
    () => () => {
      if (isPanelOpenRef.current) {
        log.debug('Closing panel', panelId);
        LayoutUtils.closeComponent(parent, { id: panelId });
        isPanelOpenRef.current = false;
        onClose(panelId);
      }
    },
    [parent, onClose, panelId]
  );

  const handlePanelClosed = useCallback(
    closedPanelId => {
      if (closedPanelId === panelId) {
        log.debug('Panel closed', panelId);
        isPanelOpenRef.current = false;
        onClose(panelId);
      }
    },
    [onClose, panelId]
  );

  useListener(eventHub, PanelEvent.CLOSED, handlePanelClosed);

  // useEffect(() => {
  //   console.log('XXX listenForPortalOpened', panelId);
  //   listenForPortalOpened(eventHub, ({ container, element: newElement }) => {
  //     if (LayoutUtils.getIdFromContainer(container) === panelId) {
  //       log.debug2('Panel opened', panelId, newElement);
  //       setElement(newElement);
  //     }
  //   });
  // }, [eventHub, panelId]);

  // useEffect(
  //   () =>
  //     listenForPortalClosed(eventHub, ({ container }) => {
  //       if (LayoutUtils.getIdFromContainer(container) === panelId) {
  //         isPanelOpenRef.current = false;
  //         setElement(undefined);
  //       }
  //     }),
  //   [eventHub, panelId]
  // );

  useEffect(() => {
    if (
      isPanelOpenRef.current === false ||
      openedWidgetRef.current !== widget
    ) {
      if (isPanelOpenRef.current === false) {
        const existingStack = LayoutUtils.getStackForConfig(parent, {
          id: panelId,
        });
        if (existingStack != null) {
          log.debug2('Panel already exists, just re-using');
          isPanelOpenRef.current = true;
          return;
        }
      }

      const panelTitle = title ?? widget.name ?? '';
      const config = {
        type: 'react-component' as const,
        component: PortalPanel.displayName,
        props: {},
        title: panelTitle,
        id: panelId,
      };

      LayoutUtils.openComponent({ root: parent, config });
      log.debug('Opened panel', panelId, config);
      isPanelOpenRef.current = true;
      openedWidgetRef.current = widget;

      onOpen(panelId);
    }
  }, [parent, widget, onOpen, panelId, title]);

  return element
    ? ReactDOM.createPortal(
        <ScopedIdWrapper id={panelId}>
          <ReactPanelContext.Provider value={panelId}>
            {children}
          </ReactPanelContext.Provider>
        </ScopedIdWrapper>,
        element
      )
    : null;
}

export default ReactPanel;
