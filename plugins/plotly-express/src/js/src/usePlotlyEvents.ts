import { useEffect } from 'react';
import type { PlotlyHTMLElement } from 'plotly.js';
import {
  attachEventListeners,
  detachEventListeners,
} from './PlotlyEventBridge.js';
import type PlotlyExpressChartModel from './PlotlyExpressChartModel.js';

const POLL_INTERVAL_MS = 50;
const POLL_TIMEOUT_MS = 5000;

/**
 * Find the plotly graph div under `container`. The Chart component from
 * @deephaven/chart inserts the plotly node asynchronously, so we poll for it
 * after mount/revision change. Plotly tags its root with the `js-plotly-plot`
 * class and exposes `on` / `removeListener`.
 */
function findPlotElement(
  container: HTMLDivElement | null
): PlotlyHTMLElement | null {
  if (!container) return null;
  return container.querySelector<PlotlyHTMLElement>('.js-plotly-plot');
}

/**
 * Attach plotly DOM listeners for every event the server has registered
 * a handler for. Re-runs when the model or revision changes (handlers can
 * be re-declared on each NEW_FIGURE message).
 */
export function usePlotlyEvents(
  model: PlotlyExpressChartModel | undefined,
  container: HTMLDivElement | null,
  widgetRevision: number
): void {
  useEffect(() => {
    if (!model || !container) return undefined;

    let cancelled = false;
    let timeoutId: number | undefined;
    let attachedTo: PlotlyHTMLElement | null = null;

    const sendEvent = (eventType: string, data: unknown) => {
      model.sendEvent(eventType, data);
    };

    const tryAttach = () => {
      if (cancelled) return;
      const plotEl = findPlotElement(container);
      if (plotEl && typeof plotEl.on === 'function') {
        attachEventListeners(
          plotEl,
          model.getRegisteredEvents(),
          sendEvent as Parameters<typeof attachEventListeners>[2]
        );
        attachedTo = plotEl;
        return;
      }
      timeoutId = window.setTimeout(tryAttach, POLL_INTERVAL_MS);
    };

    const giveUpId = window.setTimeout(() => {
      cancelled = true;
      if (timeoutId !== undefined) window.clearTimeout(timeoutId);
    }, POLL_TIMEOUT_MS);

    tryAttach();

    return () => {
      cancelled = true;
      if (timeoutId !== undefined) window.clearTimeout(timeoutId);
      window.clearTimeout(giveUpId);
      if (attachedTo) {
        detachEventListeners(attachedTo);
      }
    };
  }, [model, container, widgetRevision]);
}

export default usePlotlyEvents;
