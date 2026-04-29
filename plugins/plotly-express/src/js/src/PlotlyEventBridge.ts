/**
 * Bridges plotly.js DOM events back to the server. The server tells the
 * client which event names have a Python handler registered (via
 * `widgetData.figure.deephaven.events`); we attach Plotly listeners only for
 * those events and round-trip them as `{type: "EVENT", event_type, data}`
 * messages on the existing widget channel.
 *
 * Point payloads are sanitized to a small whitelist before sending: the raw
 * plotly point objects contain circular references back to `data` and
 * `fullData` which cannot be JSON-stringified, and which leak large amounts
 * of bytes we do not want on the wire.
 */
import type { PlotlyHTMLElement } from 'plotly.js';

export const EVENT_NAMES = [
  'click',
  'select',
  'deselect',
  'hover',
  'unhover',
  'relayout',
  'legend_click',
] as const;

export type EventName = (typeof EVENT_NAMES)[number];

/** snake_case event name → plotly.js event identifier. */
const PLOTLY_EVENT_BY_NAME: Record<EventName, string> = {
  click: 'plotly_click',
  select: 'plotly_selected',
  deselect: 'plotly_deselect',
  hover: 'plotly_hover',
  unhover: 'plotly_unhover',
  relayout: 'plotly_relayout',
  legend_click: 'plotly_legendclick',
};

const POINT_FIELD_WHITELIST = [
  'curveNumber',
  'pointNumber',
  'pointIndex',
  'pointIndices',
  'x',
  'y',
  'z',
  'lat',
  'lon',
  'location',
  'label',
  'value',
  'hovertext',
  'customdata',
] as const;

export type SendEventFn = (eventType: EventName, data: unknown) => void;

interface AttachedListener {
  event: EventName;
  plotlyEvent: string;
  handler: (eventData: unknown) => void;
}

const ATTACHED_BY_PLOT = new WeakMap<PlotlyHTMLElement, AttachedListener[]>();

function sanitizePoint(point: unknown): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  if (point === null || point === undefined || typeof point !== 'object') {
    return out;
  }
  const p = point as Record<string, unknown>;
  POINT_FIELD_WHITELIST.forEach(key => {
    if (p[key] !== undefined) {
      out[key] = p[key];
    }
  });
  return out;
}

function sanitizePoints(points: unknown): Array<Record<string, unknown>> {
  if (!Array.isArray(points)) return [];
  return points.map(sanitizePoint);
}

function buildPayload(
  event: EventName,
  eventData: unknown
): Record<string, unknown> {
  // Plotly hands us a different shape per event. Pull out only what's safe to
  // ship; drop refs to data/fullData/xaxis/etc. which carry circular state.
  if (event === 'relayout') {
    return { relayout: eventData ?? {} };
  }
  if (event === 'legend_click') {
    const e = (eventData ?? {}) as Record<string, unknown>;
    return {
      legend: {
        curveNumber: e.curveNumber,
        // `visible` is on the trace prior to the click; the toggled value is
        // computed by plotly internally — we forward the pre-click state.
        visible: ((e.data as Array<Record<string, unknown>> | undefined) ?? [])[
          (e.curveNumber as number) ?? -1
        ]?.visible,
      },
    };
  }
  const e = (eventData ?? {}) as Record<string, unknown>;
  const payload: Record<string, unknown> = {
    points: sanitizePoints(e.points),
  };
  if (event === 'select' && e.range !== undefined) {
    payload.selection = { range: e.range };
  }
  if (event === 'select' && e.lassoPoints !== undefined) {
    payload.selection = { lassoPoints: e.lassoPoints };
  }
  return payload;
}

/**
 * Attach plotly listeners for every event named in `registeredEvents`. Any
 * existing listeners on `plotEl` from a previous call are detached first so
 * this is safe to call repeatedly when the model/widget revision changes.
 */
export function attachEventListeners(
  plotEl: PlotlyHTMLElement | null,
  registeredEvents: ReadonlySet<string>,
  sendEvent: SendEventFn
): void {
  if (!plotEl) return;
  detachEventListeners(plotEl);
  if (registeredEvents.size === 0) return;

  const attached: AttachedListener[] = [];
  EVENT_NAMES.forEach(event => {
    if (!registeredEvents.has(event)) return;
    const plotlyEvent = PLOTLY_EVENT_BY_NAME[event];
    const handler = (eventData: unknown) => {
      sendEvent(event, buildPayload(event, eventData));
    };
    plotEl.on(
      plotlyEvent as Parameters<PlotlyHTMLElement['on']>[0],
      handler as Parameters<PlotlyHTMLElement['on']>[1]
    );
    attached.push({ event, plotlyEvent, handler });
  });
  ATTACHED_BY_PLOT.set(plotEl, attached);
}

export function detachEventListeners(plotEl: PlotlyHTMLElement | null): void {
  if (!plotEl) return;
  const attached = ATTACHED_BY_PLOT.get(plotEl);
  if (!attached) return;
  // We own these plotly_* event names exclusively (each maps from one of our
  // snake_case event types and we attach exactly one listener per name), so
  // removeAllListeners is precise here. The plotly.js type defs do not
  // export `removeListener`, only `removeAllListeners`.
  attached.forEach(({ plotlyEvent }) => {
    plotEl.removeAllListeners(plotlyEvent);
  });
  ATTACHED_BY_PLOT.delete(plotEl);
}

// Test-only helpers, exported under a deliberately ugly name so consumers
// don't reach for them in production code paths.
// eslint-disable-next-line no-underscore-dangle
export const TEST_ONLY = {
  buildPayload,
  sanitizePoint,
  sanitizePoints,
  PLOTLY_EVENT_BY_NAME,
};
