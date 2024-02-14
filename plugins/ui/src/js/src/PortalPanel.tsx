import React, { useEffect, useRef } from 'react';
import { DashboardPanelProps, LayoutUtils } from '@deephaven/dashboard';
import { Panel } from '@deephaven/dashboard-core-plugins';
import { emitPortalClosed, emitPortalOpened } from './PortalPanelEvent';

/**
 * Adds and tracks a panel to the GoldenLayout.
 * Takes an HTMLElement that can be used as a Portal in another component.
 */
function PortalPanel({
  glContainer,
  glEventHub,
}: DashboardPanelProps): JSX.Element {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const { current } = ref;
    if (current == null) {
      return;
    }
    console.log(
      'XXX Portal opened',
      LayoutUtils.getIdFromContainer(glContainer)
    );
    emitPortalOpened(glEventHub, { container: glContainer, element: current });

    return () => {
      console.log(
        'XXX Portal closed',
        LayoutUtils.getIdFromContainer(glContainer)
      );
      emitPortalClosed(glEventHub, { container: glContainer });
    };
  }, [glContainer, glEventHub]);

  return (
    <Panel glContainer={glContainer} glEventHub={glEventHub}>
      <div className="ui-portal-panel" ref={ref} />
    </Panel>
  );
}

PortalPanel.displayName = '@deephaven/js-plugin-ui/PortalPanel';

export default PortalPanel;
