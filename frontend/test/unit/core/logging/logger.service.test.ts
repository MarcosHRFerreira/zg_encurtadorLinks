import LoggerService, { type LogLevel } from 'app/core/logging/logger.service';
import sinon from 'sinon';

describe('LoggerService', () => {
  let service: LoggerService;
  let consoleDebug: sinon.SinonSpy;
  let consoleInfo: sinon.SinonSpy;
  let consoleWarn: sinon.SinonSpy;
  let consoleError: sinon.SinonSpy;

  beforeEach(() => {
    // Primeiro cria os spies para capturar as chamadas, depois instancia o serviço,
    // pois o serviço faz bind dos métodos de console na inicialização.
    consoleDebug = sinon.spy(console, 'debug');
    consoleInfo = sinon.spy(console, 'info');
    consoleWarn = sinon.spy(console, 'warn');
    consoleError = sinon.spy(console, 'error');
    
    service = new LoggerService();
  });

  afterEach(() => {
    consoleDebug.restore();
    consoleInfo.restore();
    consoleWarn.restore();
    consoleError.restore();
  });

  const cases: Array<{ level: LogLevel; message: string; meta?: { context?: string; payload?: unknown }; expectedPrefix: string; hasPayload: boolean }> = [
    { level: 'debug', message: 'msg', expectedPrefix: 'msg', hasPayload: false },
    { level: 'info', message: 'info', meta: { context: 'Ctx' }, expectedPrefix: '[Ctx] info', hasPayload: false },
    { level: 'warn', message: 'warn', meta: { payload: { a: 1 } }, expectedPrefix: 'warn', hasPayload: true },
    { level: 'error', message: 'err', meta: { context: 'Http', payload: { status: 500 } }, expectedPrefix: '[Http] err', hasPayload: true },
  ];

  it.each(cases)('%s: should log with correct format', ({ level, message, meta, expectedPrefix, hasPayload }) => {
    // Arrange
    const call = (msg: string, m?: { context?: string; payload?: unknown }) => {
      switch (level) {
        case 'debug':
          service.debug(msg, m);
          break;
        case 'info':
          service.info(msg, m);
          break;
        case 'warn':
          service.warn(msg, m);
          break;
        case 'error':
          service.error(msg, m);
          break;
        default:
          throw new Error('invalid level');
      }
    };

    // Act
    call(message, meta);

    // Assert
    const spy = level === 'debug' ? consoleDebug : level === 'info' ? consoleInfo : level === 'warn' ? consoleWarn : consoleError;
    expect(spy.calledOnce).toBe(true);
    const args = spy.firstCall.args;
    expect(typeof args[0]).toBe('string');
    expect(String(args[0]).startsWith(expectedPrefix)).toBe(true);
    if (hasPayload) {
      expect(args.length).toBeGreaterThanOrEqual(2);
    } else {
      expect(args.length).toBe(1);
    }
  });
});