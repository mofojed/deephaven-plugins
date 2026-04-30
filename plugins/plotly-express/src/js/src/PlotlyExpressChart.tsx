import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import Plotly from 'plotly.js-dist-min';
import { Chart } from '@deephaven/chart';
import type { dh } from '@deephaven/jsapi-types';
import { type WidgetComponentProps } from '@deephaven/plugin';
import { useApi } from '@deephaven/jsapi-bootstrap';
import { getSettings, type RootState } from '@deephaven/redux';
import PlotlyExpressChartModel from './PlotlyExpressChartModel.js';
import { useHandleSceneTicks } from './useHandleSceneTicks.js';
import { usePlotlyEvents } from './usePlotlyEvents.js';

export function PlotlyExpressChart(
  props: WidgetComponentProps<dh.Widget>
): JSX.Element | null {
  const dh = useApi();
  const { fetch } = props;
  // Callback ref + state so that hooks downstream of the wrapper actually
  // see the mounted DOM node. A bare useRef does not trigger re-renders
  // when assigned, so an effect keyed on `ref.current` would only ever
  // observe the initial null value.
  const [container, setContainer] = useState<HTMLDivElement | null>(null);
  const [model, setModel] = useState<PlotlyExpressChartModel>();
  const settings = useSelector(getSettings<RootState>);
  const [widgetRevision, setWidgetRevision] = useState(0); // Used to force a clean chart state on widget change

  useEffect(() => {
    let cancelled = false;
    async function init() {
      const widgetData = await fetch();
      if (!cancelled) {
        setModel(new PlotlyExpressChartModel(dh, widgetData, fetch));
        setWidgetRevision(r => r + 1);
      }
    }

    init();

    return () => {
      cancelled = true;
    };
  }, [dh, fetch]);

  useHandleSceneTicks(model, container);
  usePlotlyEvents(model, container, widgetRevision);

  return model ? (
    <Chart
      // eslint-disable-next-line react/jsx-props-no-spreading, @typescript-eslint/ban-ts-comment
      // @ts-ignore
      key={widgetRevision}
      containerRef={setContainer}
      model={model}
      settings={settings}
      Plotly={Plotly}
    />
  ) : null;
}

export default PlotlyExpressChart;
