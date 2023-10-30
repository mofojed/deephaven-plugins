const path = require('path');

module.exports = {
  transform: {
    '^.+\\.(ts|tsx|js|jsx)$': ['babel-jest', { rootMode: 'upward' }],
  },
  transformIgnorePatterns: [
    '/node_modules/(?!(@deephaven|monaco-editor|d3-interpolate|d3-color)/)',
  ],
  moduleNameMapper: {
    'theme-([^/]+?)\\.css(\\?inline)?$': path.join(
      __dirname,
      './__mocks__/mockTheme.js'
    ),
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      path.join(__dirname, './__mocks__/fileMock.js'),
    '^fira$': 'identity-obj-proxy',
    '^monaco-editor$': path.join(
      __dirname,
      'node_modules',
      'monaco-editor/esm/vs/editor/editor.api.js'
    ),
    // Handle monaco worker files
    '\\.worker.*$': 'identity-obj-proxy',
    // All packages use src/js/src code
    '^@deephaven/js-plugin-(.*)$': path.join(
      __dirname,
      './plugins/$1/src/js/src'
    ),
  },
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: [path.join(__dirname, './jest.setup.ts')],
};
