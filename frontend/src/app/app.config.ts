import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { routes } from './app.routes';
import { ApiBaseUrlInterceptor } from './core/http/api-base-url.interceptor';
import ErrorLoggerInterceptor from './core/http/error-logger.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: ApiBaseUrlInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorLoggerInterceptor, multi: true },
  ],
};
