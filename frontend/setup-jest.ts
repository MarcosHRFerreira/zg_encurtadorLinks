import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

// Inicializa o ambiente de testes do Angular com Zone.js
setupZoneTestEnv();

// Mocks/globais adicionais
Object.defineProperty(window, 'scrollTo', { value: () => null, writable: true });