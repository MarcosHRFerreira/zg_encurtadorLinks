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

    // Detecta dev-server (localhost:4201). Em desenvolvimento, sempre usa URL relativa
    // para que o proxy do Angular trate CORS e roteamento, ignorando API_BASE_URL.
    const isDevServer = typeof window !== 'undefined'
      && /^localhost$|^127\.0\.0\.1$/.test(window.location.hostname)
      && (window.location.port === '4201' || window.location.port === '4203');

    if (isDevServer) {
      return next.handle(req);
    }

    // Em outros ambientes (produção), prefere a base configurada via env.js.
    const baseFromEnv = typeof window !== 'undefined' && window.__ENV__?.API_BASE_URL
      ? String(window.__ENV__?.API_BASE_URL).replace(/\/$/, '')
      : undefined;

    if (!baseFromEnv) {
      // Sem base configurada via env.js: não reescreve a URL
      return next.handle(req);
    }

    // Em produção, removemos o prefixo "/api" para alinhar com os endpoints reais do backend
    const path = req.url.replace(/^\/api(\/|$)/, '/');
    const url = `${baseFromEnv}${path}`;
    return next.handle(req.clone({ url }));
  }
}