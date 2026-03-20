import React, { useMemo } from 'react';
import {
  useLayoutManager,
  usePanelId as useLayoutPanelId,
} from '@deephaven/dashboard';
import { ElementIdProps, type DashboardElementProps } from './LayoutUtils';
import { usePanelId as useReactPanelId } from './ReactPanelContext';
import NestedDashboard from './NestedDashboard';
import DashboardContent from './DashboardContent';

/**
 * Dashboard component that can work at top-level or nested inside a panel.
 *
 * When top-level: Uses the existing layout manager's root (current behavior)
 * When nested: Delegates to NestedDashboard which creates its own GoldenLayout
 */
function Dashboard({
  children,
  ...otherProps
}: DashboardElementProps & ElementIdProps): JSX.Element | null {
  const layoutManager = useLayoutManager();
  const contextPanelId = useLayoutPanelId();
  const reactPanelId = useReactPanelId();
  const isFirstWidget = false;
  // const isFirstWidget = useMemo(
  //   () => layoutManager.root.contentItems.length <= 1,
  //   [layoutManager]
  // );
  // if (isFirstWidget) {
  //   // Just change it so headers don't show... this can be done in Enterprise maybe? Would simplify things _a lot_
  //   layoutManager.root.contentItems.forEach(item => item.remove());
  // }
  // We don't want to treat it as nested if we're the only widget in this layout, just take over the dashboard
  const isNested =
    !isFirstWidget && (contextPanelId != null || reactPanelId != null);
  if (isNested) {
    // eslint-disable-next-line react/jsx-props-no-spreading
    return <NestedDashboard {...otherProps}>{children}</NestedDashboard>;
  }

  return <DashboardContent>{children}</DashboardContent>;
}

export default Dashboard;
