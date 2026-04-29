import {
  attachEventListeners,
  detachEventListeners,
  EVENT_NAMES,
  TEST_ONLY,
} from './PlotlyEventBridge';

const { buildPayload, sanitizePoint, sanitizePoints, PLOTLY_EVENT_BY_NAME } =
  TEST_ONLY;

describe('PlotlyEventBridge sanitization', () => {
  test('sanitizePoint forwards whitelisted fields', () => {
    const point = {
      curveNumber: 0,
      pointIndex: 5,
      x: 10,
      y: 20,
      lat: 39.5,
      lon: -98.35,
      location: 'USA',
      hovertext: 'United States',
      customdata: ['USA', 21000000],
    };
    expect(sanitizePoint(point)).toEqual(point);
  });

  test('sanitizePoint strips non-whitelisted (potentially circular) fields', () => {
    const point = {
      curveNumber: 0,
      pointIndex: 5,
      x: 10,
      // Plotly attaches circular refs to these — must be dropped:
      data: { circular: 'ref' },
      fullData: { circular: 'ref' },
      xaxis: {},
      yaxis: {},
      bbox: {},
    };
    const cleaned = sanitizePoint(point);
    expect(cleaned).toEqual({ curveNumber: 0, pointIndex: 5, x: 10 });
    expect(cleaned).not.toHaveProperty('data');
    expect(cleaned).not.toHaveProperty('fullData');
  });

  test('sanitizePoints handles non-array input gracefully', () => {
    expect(sanitizePoints(undefined)).toEqual([]);
    expect(sanitizePoints(null)).toEqual([]);
    expect(sanitizePoints('not an array')).toEqual([]);
  });
});

describe('PlotlyEventBridge buildPayload', () => {
  test('click forwards sanitized points', () => {
    const event = {
      points: [{ curveNumber: 0, pointIndex: 1, location: 'USA', data: {} }],
    };
    expect(buildPayload('click', event)).toEqual({
      points: [{ curveNumber: 0, pointIndex: 1, location: 'USA' }],
    });
  });

  test('select with range produces a selection block', () => {
    const event = {
      points: [{ curveNumber: 0, pointIndex: 0 }],
      range: { x: [0, 100], y: [0, 100] },
    };
    expect(buildPayload('select', event)).toEqual({
      points: [{ curveNumber: 0, pointIndex: 0 }],
      selection: { range: { x: [0, 100], y: [0, 100] } },
    });
  });

  test('select with lassoPoints produces a selection block', () => {
    const event = {
      points: [],
      lassoPoints: { x: [1, 2, 3], y: [1, 2, 3] },
    };
    expect(buildPayload('select', event)).toEqual({
      points: [],
      selection: { lassoPoints: { x: [1, 2, 3], y: [1, 2, 3] } },
    });
  });

  test('relayout payload carries the relayout dict', () => {
    const event = { 'xaxis.range[0]': 0, 'xaxis.range[1]': 1 };
    expect(buildPayload('relayout', event)).toEqual({ relayout: event });
  });

  test('legend_click forwards curveNumber and pre-click visibility', () => {
    const event = {
      curveNumber: 1,
      data: [
        { name: 'a', visible: true },
        { name: 'b', visible: 'legendonly' },
      ],
    };
    expect(buildPayload('legend_click', event)).toEqual({
      legend: { curveNumber: 1, visible: 'legendonly' },
    });
  });
});

describe('PlotlyEventBridge name mapping', () => {
  test('every snake_case event maps to a plotly_* identifier', () => {
    EVENT_NAMES.forEach(name => {
      expect(PLOTLY_EVENT_BY_NAME[name]).toMatch(/^plotly_/);
    });
  });

  test('legend_click maps to plotly_legendclick (no underscore)', () => {
    expect(PLOTLY_EVENT_BY_NAME.legend_click).toBe('plotly_legendclick');
  });
});

describe('PlotlyEventBridge attach/detach', () => {
  function makeFakePlot() {
    const handlers = new Map<string, (eventData: unknown) => void>();
    const on = jest.fn((evt: string, fn: (data: unknown) => void) => {
      handlers.set(evt, fn);
    });
    const removeAllListeners = jest.fn((evt: string) => {
      handlers.delete(evt);
    });
    return { on, removeAllListeners, handlers };
  }

  test('no-op when registeredEvents is empty', () => {
    const plot = makeFakePlot();
    const send = jest.fn();
    attachEventListeners(plot as never, new Set(), send);
    expect(plot.on).not.toHaveBeenCalled();
  });

  test('attaches only the events the server registered', () => {
    const plot = makeFakePlot();
    const send = jest.fn();
    attachEventListeners(plot as never, new Set(['click', 'select']), send);
    expect(plot.on).toHaveBeenCalledWith('plotly_click', expect.any(Function));
    expect(plot.on).toHaveBeenCalledWith(
      'plotly_selected',
      expect.any(Function)
    );
    expect(plot.on).toHaveBeenCalledTimes(2);

    // Firing the click handler calls send() with sanitized payload
    plot.handlers.get('plotly_click')?.({
      points: [{ curveNumber: 0, pointIndex: 7, location: 'USA' }],
    });
    expect(send).toHaveBeenCalledWith('click', {
      points: [{ curveNumber: 0, pointIndex: 7, location: 'USA' }],
    });
  });

  test('detach removes previously attached listeners', () => {
    const plot = makeFakePlot();
    const send = jest.fn();
    attachEventListeners(plot as never, new Set(['click']), send);
    detachEventListeners(plot as never);
    expect(plot.removeAllListeners).toHaveBeenCalledWith('plotly_click');
  });

  test('attach is idempotent — re-attach detaches previous first', () => {
    const plot = makeFakePlot();
    const send = jest.fn();
    attachEventListeners(plot as never, new Set(['click']), send);
    attachEventListeners(plot as never, new Set(['click', 'hover']), send);
    // First call's listener was removed before re-attaching
    expect(plot.removeAllListeners).toHaveBeenCalledWith('plotly_click');
    expect(plot.on).toHaveBeenCalledWith('plotly_click', expect.any(Function));
    expect(plot.on).toHaveBeenCalledWith('plotly_hover', expect.any(Function));
  });
});
