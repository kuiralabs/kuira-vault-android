import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'node',
    include: ['test/**/*.test.ts'],
    reporters: 'verbose',
    coverage: {
      provider: 'v8',
      reporter: ['text'],
      // Coverage targets the test harness TS (the .compact source maps are too
      // noisy to gate meaningfully — pragmas/ledger decls read as uncovered).
      include: ['test/**/*.ts'],
      exclude: ['test/**/*.test.ts'],
    },
  },
});
