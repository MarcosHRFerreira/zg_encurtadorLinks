import { Injectable } from '@angular/core';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';
export type LoggerMeta = Readonly<{
  context?: string;
  payload?: unknown;
}>;

@Injectable({ providedIn: 'root' })
export default class LoggerService {
  private readonly levels: Readonly<Record<LogLevel, (message?: unknown, ...optionalParams: unknown[]) => void>> = {
    debug: console.debug.bind(console),
    info: console.info.bind(console),
    warn: console.warn.bind(console),
    error: console.error.bind(console),
  } as const;

  debug(message: string, meta?: LoggerMeta): void { this.log('debug', message, meta); }
  info(message: string, meta?: LoggerMeta): void { this.log('info', message, meta); }
  warn(message: string, meta?: LoggerMeta): void { this.log('warn', message, meta); }
  error(message: string, meta?: LoggerMeta): void { this.log('error', message, meta); }

  private log(level: LogLevel, message: string, meta?: LoggerMeta): void {
    const fn = this.levels[level];
    const ctx = meta?.context ? `[${meta.context}]` : '';
    const payload = meta?.payload;
    if (typeof payload === 'undefined') {
      fn(`${ctx} ${message}`.trim());
    } else {
      fn(`${ctx} ${message}`.trim(), payload);
    }
  }
}