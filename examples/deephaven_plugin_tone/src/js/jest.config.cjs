module.exports = {
  testEnvironment: 'jsdom',
  testMatch: ['<rootDir>/src/**/*.test.ts'],
  // @deephaven packages ship ESM, so they must be transformed for jest.
  transform: {
    '^.+\\.(ts|js)$': ['ts-jest', { tsconfig: { allowJs: true } }],
  },
  transformIgnorePatterns: ['node_modules/(?!(@deephaven)/)'],
};
