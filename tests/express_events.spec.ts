import { expect, test } from '@playwright/test';
import { gotoPage, openPanel, SELECTORS } from './utils';

// Smoke tests that verify plotly-express figures render cleanly when
// event-handler kwargs (on_click, on_select, ...) are present. The handler
// dispatch round-trip is exercised by Python and JS unit tests; here we just
// confirm that registering handlers does not break the chart.
test.describe('plotly-express event handlers', () => {
  ['express_event_fig', 'express_event_bar'].forEach(name => {
    test(`${name} renders with handlers registered`, async ({ page }) => {
      await gotoPage(page, '');
      await openPanel(page, name, SELECTORS.REACT_PANEL_VISIBLE);

      // Plotly graph div is present under the panel body — chart finished
      // mounting and event listeners (if any) are attached.
      const panel = page.locator(SELECTORS.REACT_PANEL_VISIBLE);
      await expect(panel.locator('.js-plotly-plot')).toBeVisible();

      // No error overlay surfaced from the listener attaching.
      await expect(panel.locator('.chart-panel-error')).toHaveCount(0);
    });
  });
});
