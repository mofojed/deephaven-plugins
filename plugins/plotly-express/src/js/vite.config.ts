/* eslint-disable import/no-extraneous-dependencies */
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import cssInjectedByJsPlugin from 'vite-plugin-css-injected-by-js';

// The host application provides these singletons at runtime via the import map
// injected by @deephaven/app-utils. They must NOT be bundled into the plugin so
// that the plugin shares the host's instances (one React, one redux store, one
// design system, ...). Keep this list in sync with the host's resolve map
// (web-client-ui remote-component.config.ts).
const external = [
  'react',
  'react-dom',
  'redux',
  'react-redux',
  '@deephaven/chart',
  '@deephaven/components',
  '@deephaven/dashboard',
  '@deephaven/dashboard-core-plugins',
  '@deephaven/icons',
  '@deephaven/jsapi-bootstrap',
  '@deephaven/jsapi-utils',
  '@deephaven/log',
  '@deephaven/plugin',
];

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  build: {
    minify: false,
    outDir: 'dist/bundle',
    // Emit per-chunk CSS so lazy chunks carry their own styles.
    cssCodeSplit: true,
    lib: {
      entry: './src/index.ts',
      fileName: () => 'index.js',
      // ESM so dynamic import() produces on-demand chunks that lazy-load the
      // heavy Chart component and Plotly runtime only when a chart is shown.
      formats: ['es'],
    },
    rollupOptions: {
      external,
    },
  },
  define:
    mode === 'production' ? { 'process.env.NODE_ENV': '"production"' } : {},
  plugins: [
    react(),
    // Inject each chunk's CSS via JS so styles for lazy-loaded chunks are added
    // to the document when the chunk is dynamically imported, rather than
    // requiring a separate <link> the host would have to know about.
    cssInjectedByJsPlugin({ relativeCSSInjection: true }),
  ],
}));
