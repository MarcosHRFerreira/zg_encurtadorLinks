import { HttpErrorResponse, HttpHandler, HttpRequest } from '@angular/common/http';
import ErrorLoggerInterceptor from 'app/core/http/error-logger.interceptor';
import LoggerService from 'app/core/logging/logger.service';
import sinon from 'sinon';
import { of, throwError } from 'rxjs';

class DummyHandler implements HttpHandler {
  constructor(private readonly responder: (req: HttpRequest<unknown>) => any) {}
  handle(req: HttpRequest<unknown>) {
    return this.responder(req);
  }
}

describe('ErrorLoggerInterceptor', () => {
  let interceptor: ErrorLoggerInterceptor;
  let logger: LoggerService;
  let errorSpy: sinon.SinonSpy<[message: string, meta?: unknown], void>;

  beforeEach(() => {
    logger = new LoggerService();
    interceptor = new ErrorLoggerInterceptor(logger);
    errorSpy = sinon.spy(logger, 'error');
  });

  afterEach(() => {
    errorSpy.restore();
  });

  it('should log HttpErrorResponse with status and metadata', (done) => {
    // Arrange
    const req = new HttpRequest('GET', '/test');
    const handler = new DummyHandler(() => throwError(() => new HttpErrorResponse({ status: 404, statusText: 'Not Found', url: '/test' })));

    // Act
    interceptor.intercept(req, handler).subscribe({
      next: () => done.fail('should not emit next'),
      error: () => {
        // Assert
        expect(errorSpy.calledOnce).toBe(true);
        const [msg, meta] = errorSpy.firstCall.args as [string, { context?: string; payload?: any }];
        expect(msg).toContain('HTTP 404 GET /test');
        expect(meta?.context).toBe('Http');
        expect(meta?.payload?.status).toBe(404);
        done();
      }
    });
  });

  it('should log unknown error types', (done) => {
    // Arrange
    const req = new HttpRequest('POST', '/x');
    const handler = new DummyHandler(() => throwError(() => new Error('oops')));

    // Act
    interceptor.intercept(req, handler).subscribe({
      next: () => done.fail('should not emit next'),
      error: () => {
        // Assert
        expect(errorSpy.calledOnce).toBe(true);
        const [msg, meta] = errorSpy.firstCall.args as [string, { context?: string; payload?: any }];
        expect(msg).toContain('HTTP error POST /x');
        expect(meta?.context).toBe('Http');
        expect(meta?.payload?.url).toBe('/x');
        done();
      }
    });
  });

  it('should not log on successful responses', (done) => {
    // Arrange
    const req = new HttpRequest('GET', '/ok');
    const handler = new DummyHandler(() => of({} as any));

    // Act
    interceptor.intercept(req, handler).subscribe({
      next: () => {
        // Assert
        expect(errorSpy.notCalled).toBe(true);
        done();
      },
      error: (e) => done.fail(String(e))
    });
  });
});