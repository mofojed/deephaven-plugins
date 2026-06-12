import React, { Suspense } from 'react';
import { LoadingOverlay } from '@deephaven/components';
import type { dh } from '@deephaven/jsapi-types';
import { type WidgetComponentProps } from '@deephaven/plugin';

// Lazy-load the chart view so the heavy `Chart` component and `Plotly` runtime
// are split into a separate chunk that is only fetched when the first chart is
// rendered. Subsequent charts reuse the already-loaded chunk.
const PlotlyExpressChartView = React.lazy(
  () => import('./PlotlyExpressChartView.js')
);

export function PlotlyExpressChart(
  props: WidgetComponentProps<dh.Widget>
): JSX.Element {
  return (
    <Suspense fallback={<LoadingOverlay />}>
      {/* eslint-disable-next-line react/jsx-props-no-spreading */}
      <PlotlyExpressChartView {...props} />
    </Suspense>
  );
}

export default PlotlyExpressChart;
