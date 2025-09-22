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

    // Preferir env; se não houver, deixar URL relativa para o proxy do dev-server tratar (evita CORS em desenvolvimento)
    const baseFromEnv = typeof window !== 'undefined' && window.__ENV__?.API_BASE_URL
      ? String(window.__ENV__?.API_BASE_URL).replace(/\/$/, '')
      : undefined;

    if (!baseFromEnv) {
      // Sem base configurada via env.js: não reescreve a URL
      return next.handle(req);
    }

    const url = `${baseFromEnv}${req.url}`;
    return next.handle(req.clone({ url }));
  }
}