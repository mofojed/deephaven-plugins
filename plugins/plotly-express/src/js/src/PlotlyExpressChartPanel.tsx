import React, { Suspense } from 'react';
import { LoadingOverlay } from '@deephaven/components';
import type { dh } from '@deephaven/jsapi-types';
import { type WidgetPanelProps } from '@deephaven/plugin';

// Lazy-load the chart panel view so the heavy `ChartPanel` component and
// `Plotly` runtime are split into a separate chunk that is only fetched when
// the first chart panel is rendered. Subsequent panels reuse the loaded chunk.
const PlotlyExpressChartPanelView = React.lazy(
  () => import('./PlotlyExpressChartPanelView.js')
);

export function PlotlyExpressChartPanel(
  props: WidgetPanelProps<dh.Widget>
): JSX.Element {
  return (
    <Suspense fallback={<LoadingOverlay />}>
      {/* eslint-disable-next-line react/jsx-props-no-spreading */}
      <PlotlyExpressChartPanelView {...props} />
    </Suspense>
  );
}

export default PlotlyExpressChartPanel;
