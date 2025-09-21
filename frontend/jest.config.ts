/** @jest-config-loader ts-node */
import type { Config } from 'jest';

const config: Config = {
  preset: 'jest-preset-angular',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testMatch: ['<rootDir>/test/**/*.test.ts'],
  testPathIgnorePatterns: ['/node_modules/', '/dist/', '<rootDir>/src/test.ts'],
  moduleNameMapper: {
    '^app/(.*)$': '<rootDir>/src/app/$1',
    '^src/(.*)$': '<rootDir>/src/$1'
  },
  restoreMocks: true,
  clearMocks: true,
  resetMocks: true,
  collectCoverage: true,
  collectCoverageFrom: ['src/**/*.ts', '!src/main.ts', '!src/app/app.routes.ts'],
  coverageDirectory: '<rootDir>/coverage',
};

export default config;