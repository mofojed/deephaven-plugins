import { expect, test } from '@playwright/test';
import { gotoPage, openPanel } from './utils';

// End-to-end verification that a plotly click on a chart with a registered
// `on_click=` handler round-trips a sanitized EVENT message. The model
// stashes the most recent sendEvent payload on window for assertion.
async function waitForPlotReady(page: import('@playwright/test').Page) {
  await page.waitForFunction(() => {
    const el = document.querySelector('.js-plotly-plot') as
      | (HTMLElement & { on?: unknown })
      | null;
    return el != null && typeof el.on === 'function';
  });
  // Give usePlotlyEvents a moment to attach (it polls at 50ms).
  await page.waitForTimeout(200);
}

async function emitPlotlyClick(
  page: import('@playwright/test').Page,
  point: Record<string, unknown>
) {
  await page.evaluate(p => {
    /* eslint-disable no-underscore-dangle */
    const w = window as unknown as Record<string, unknown>;
    w.__plotlyExpressLastEvent = null;
    /* eslint-enable no-underscore-dangle */
    const el = document.querySelector('.js-plotly-plot') as
      | (HTMLElement & {
          emit?: (evt: string, data: unknown) => void;
        })
      | null;
    if (el == null || typeof el.emit !== 'function') {
      throw new Error('plot element does not expose emit()');
    }
    el.emit('plotly_click', { points: [p] });
  }, point);
}

async function readLastEvent(page: import('@playwright/test').Page) {
  return page.evaluate(() => {
    /* eslint-disable no-underscore-dangle */
    return (window as unknown as Record<string, unknown>)
      .__plotlyExpressLastEvent;
    /* eslint-enable no-underscore-dangle */
  });
}

test.describe('plotly-express event handlers', () => {
  test('scatter_geo with on_click attaches a listener and click sends EVENT', async ({
    page,
  }) => {
    await gotoPage(page, '');
    await openPanel(page, 'express_event_fig', '.js-plotly-plot');

    await expect(
      page.locator('.iris-chart-panel').locator('.js-plotly-plot')
    ).toBeVisible();
    await waitForPlotReady(page);

    await emitPlotlyClick(page, {
      curveNumber: 0,
      pointIndex: 0,
      pointNumber: 0,
      location: 'USA',
      lat: 39.5,
      lon: -98.35,
    });

    expect(await readLastEvent(page)).toMatchObject({
      eventType: 'click',
      data: {
        points: [
          expect.objectContaining({
            curveNumber: 0,
            pointIndex: 0,
            location: 'USA',
          }),
        ],
      },
    });
  });

  test('bar with on_click forwards a click', async ({ page }) => {
    await gotoPage(page, '');
    await openPanel(page, 'express_event_bar', '.js-plotly-plot');

    await expect(
      page.locator('.iris-chart-panel').locator('.js-plotly-plot')
    ).toBeVisible();
    await waitForPlotReady(page);

    await emitPlotlyClick(page, {
      curveNumber: 0,
      pointIndex: 1,
      x: 'B',
      y: 3,
    });

    expect(await readLastEvent(page)).toMatchObject({
      eventType: 'click',
      data: {
        points: [expect.objectContaining({ pointIndex: 1, x: 'B', y: 3 })],
      },
    });
  });

  test('figure without handlers emits no EVENT on click', async ({ page }) => {
    await gotoPage(page, '');
    // express_fig is the existing fixture, no on_click registered.
    await openPanel(page, 'express_fig', '.js-plotly-plot');

    await expect(
      page.locator('.iris-chart-panel').locator('.js-plotly-plot')
    ).toBeVisible();
    await waitForPlotReady(page);

    await emitPlotlyClick(page, { curveNumber: 0, pointIndex: 0 });

    expect(await readLastEvent(page)).toBeNull();
  });
});
