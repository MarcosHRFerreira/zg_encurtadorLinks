import { Injectable, computed, inject, signal } from '@angular/core';
import type { components } from 'app/core/api/types';
import { ShortenAdapter, type ShortUrlDomain } from '../data/shorten.adapter';

type ShortenRequest = components['schemas']['ShortenRequest'];

type ErrorLike = Readonly<{
  status?: number;
  statusCode?: number;
  response?: { status?: number };
  cause?: { status?: number };
  error?: { status?: number };
}> | Response;

@Injectable({ providedIn: 'root' })
export class ShortenFacade {
  private readonly adapter = inject(ShortenAdapter);

  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _result = signal<ShortUrlDomain | null>(null);

  readonly loading = computed(() => this._loading());
  readonly error = computed(() => this._error());
  readonly result = computed(() => this._result());

  reset(): void {
    this._loading.set(false);
    this._error.set(null);
    this._result.set(null);
  }

  async create(request: ShortenRequest): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    this._result.set(null);

    try {
      const res = await this.adapter.create(request);
      this._result.set(res);
    } catch (e: unknown) {
      const message = this.mapError(e as ErrorLike);
      this._error.set(message);
    } finally {
      this._loading.set(false);
    }
  }

  private mapError(e: ErrorLike): string {
    const status = this.extractStatus(e);

    if (status === 400) return 'URL inválida';
    if (status === 409) return 'Código já utilizado';

    return 'Erro ao encurtar URL';
  }

  private extractStatus(e: ErrorLike): number | undefined {
    if (typeof Response !== 'undefined' && e instanceof Response) return e.status;

    if (typeof e === 'object' && e) {
      if ('status' in e && typeof e.status === 'number') return e.status;
      if ('statusCode' in e && typeof e.statusCode === 'number') return e.statusCode;
      if ('response' in e && e.response && typeof e.response.status === 'number') return e.response.status;
      if ('cause' in e && e.cause && typeof e.cause.status === 'number') return e.cause.status;
      if ('error' in e && e.error && typeof e.error.status === 'number') return e.error.status;
    }

    return undefined;
  }
}