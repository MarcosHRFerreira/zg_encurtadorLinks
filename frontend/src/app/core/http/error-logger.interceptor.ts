import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import LoggerService from '../logging/logger.service';

@Injectable()
export default class ErrorLoggerInterceptor implements HttpInterceptor {
  constructor(private readonly logger: LoggerService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const startedAt = Date.now();
    return next.handle(req).pipe(
      tap({
        error: (err: unknown) => {
          const elapsed = Date.now() - startedAt;
          if (err instanceof HttpErrorResponse) {
            this.logger.error(`HTTP ${err.status} ${req.method} ${req.urlWithParams} (${elapsed}ms)`, {
              context: 'Http',
              payload: { status: err.status, url: req.urlWithParams, method: req.method, message: err.message, error: err.error }
            });
          } else {
            this.logger.error(`HTTP error ${req.method} ${req.urlWithParams} (${elapsed}ms)`, {
              context: 'Http',
              payload: { url: req.urlWithParams, method: req.method, error: String(err) }
            });
          }
        }
      })
    );
  }
}