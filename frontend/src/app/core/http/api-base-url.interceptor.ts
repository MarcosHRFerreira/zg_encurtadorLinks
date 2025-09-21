import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

// Ambient declaration for window.__ENV__ shape
declare global {
  interface Window { __ENV__?: { API_BASE_URL?: string } }
}

@Injectable()
export class ApiBaseUrlInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!req.url.startsWith('/')) {
      return next.handle(req);
    }

    const base = typeof window !== 'undefined' && window.__ENV__?.API_BASE_URL
      ? String(window.__ENV__?.API_BASE_URL).replace(/\/$/, '')
      : '';

    const url = base ? `${base}${req.url}` : req.url;
    return next.handle(req.clone({ url }));
  }
}