import React, { PropsWithChildren, useState } from 'react';
import {
  Dashboard as DHCDashboard,
  useDashboardPanel,
  DashboardPluginComponentProps,
  assertIsDashboardPluginProps,
} from '@deephaven/dashboard';
import { useDashboardPlugins } from '@deephaven/plugin';
import NestedDashboardContent from './NestedDashboardContent';
import { ElementIdProps } from './LayoutUtils';
import PortalPanel from './PortalPanel';

type NestedDashboardProps = PropsWithChildren<ElementIdProps>;

function NestedDashboardPlugin(
  props: Partial<DashboardPluginComponentProps>
): JSX.Element | null {
  console.log('xxx Registering portal panel...');
  assertIsDashboardPluginProps(props);
  useDashboardPanel({
    dashboardProps: props,
    componentName: PortalPanel.displayName,
    component: PortalPanel,

    // We don't want these panels to be triggered by a widget opening, we want to control how it is opened later
    supportedTypes: [],
  });

  return null;
}

/**
 * A dashboard that can be nested inside a panel.
 * Creates its own GoldenLayout instance and manages panels independently.
 */
function NestedDashboard({
  children,
  __dhId,
}: NestedDashboardProps): JSX.Element {
  const plugins = useDashboardPlugins();
  const [layoutInitialized, setLayoutInitialized] = useState(false);

  return (
    <div className="dh-nested-dashboard">
      {/* DHCDashboard creates GoldenLayout and provides LayoutManagerContext */}
      <DHCDashboard onLayoutInitialized={() => setLayoutInitialized(true)}>
        {plugins}
        <NestedDashboardPlugin />
        {layoutInitialized && (
          <NestedDashboardContent __dhId={__dhId}>
            {children}
          </NestedDashboardContent>
        )}
      </DHCDashboard>
    </div>
  );
}

NestedDashboard.displayName = 'NestedDashboard';

export default NestedDashboard;
