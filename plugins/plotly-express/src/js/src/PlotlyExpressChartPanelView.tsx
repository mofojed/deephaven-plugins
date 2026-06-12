import React, { useCallback, useState } from 'react';
import Plotly from 'plotly.js-dist-min';
import {
  ChartPanel,
  type ChartPanelProps,
} from '@deephaven/dashboard-core-plugins';
import type { dh } from '@deephaven/jsapi-types';
import { type WidgetPanelProps } from '@deephaven/plugin';
import { useApi } from '@deephaven/jsapi-bootstrap';
import PlotlyExpressChartModel from './PlotlyExpressChartModel.js';
import { useHandleSceneTicks } from './useHandleSceneTicks.js';

/**
 * Lazy-loaded view for the Plotly Express chart panel. This module statically
 * imports the heavy `ChartPanel` component and `Plotly` runtime so they are
 * emitted into a separate chunk that is only fetched when the first chart panel
 * is rendered.
 */
export function PlotlyExpressChartPanelView(
  props: WidgetPanelProps<dh.Widget>
): JSX.Element {
  const dh = useApi();
  const { fetch, metadata = {}, ...rest } = props;
  const [container, setContainer] = useState<HTMLDivElement | null>(null);
  const [model, setModel] = useState<PlotlyExpressChartModel>();

  const makeModel = useCallback(async () => {
    const widgetData = await fetch();
    const m = new PlotlyExpressChartModel(dh, widgetData, fetch);
    setModel(m);
    return m;
  }, [dh, fetch]);

  useHandleSceneTicks(model, container);

  return (
    <ChartPanel
      // eslint-disable-next-line react/jsx-props-no-spreading
      {...(rest as unknown as ChartPanelProps)}
      containerRef={setContainer}
      makeModel={makeModel}
      Plotly={Plotly}
      metadata={metadata as ChartPanelProps['metadata']}
    />
  );
}

export default PlotlyExpressChartPanelView;
