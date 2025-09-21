import { Injectable, computed, inject, signal } from '@angular/core';
import { StatsAdapter, type StatsDomain, RankingAdapter, type RankingDomainItem } from '../data/queries.adapters';

export type ErrorLike = Readonly<{
  status?: number;
  statusCode?: number;
  response?: { status?: number } | null;
  cause?: { status?: number } | null;
  error?: { status?: number } | null;
}> | Response | null | undefined;

@Injectable({ providedIn: 'root' })
export class QueriesFacade {
  private readonly statsAdapter = inject(StatsAdapter);
  private readonly rankingAdapter = inject(RankingAdapter);

  private readonly _statsLoading = signal(false);
  private readonly _statsError = signal<string | null>(null);
  private readonly _stats = signal<StatsDomain | null>(null);

  private readonly _rankingLoading = signal(false);
  private readonly _rankingError = signal<string | null>(null);
  private readonly _ranking = signal<ReadonlyArray<RankingDomainItem>>([]);

  readonly statsLoading = computed(() => this._statsLoading());
  readonly statsError = computed(() => this._statsError());
  readonly stats = computed(() => this._stats());

  readonly rankingLoading = computed(() => this._rankingLoading());
  readonly rankingError = computed(() => this._rankingError());
  readonly ranking = computed(() => this._ranking());

  resetStats(): void {
    this._statsLoading.set(false);
    this._statsError.set(null);
    this._stats.set(null);
  }

  resetRanking(): void {
    this._rankingLoading.set(false);
    this._rankingError.set(null);
    this._ranking.set([]);
  }

  async fetchStats(code: string): Promise<void> {
    this._statsLoading.set(true);
    this._statsError.set(null);
    this._stats.set(null);
    try {
      const res = await this.statsAdapter.getByCode(code);
      this._stats.set(res);
    } catch (e: unknown) {
      const message = this.mapStatsError(e as ErrorLike);
      this._statsError.set(message);
    } finally {
      this._statsLoading.set(false);
    }
  }

  async fetchRanking(): Promise<void> {
    this._rankingLoading.set(true);
    this._rankingError.set(null);
    this._ranking.set([]);
    try {
      const res = await this.rankingAdapter.list();
      this._ranking.set(res);
    } catch (_e: unknown) {
      this._rankingError.set('Erro ao carregar ranking');
    } finally {
      this._rankingLoading.set(false);
    }
  }

  private mapStatsError(e: ErrorLike): string {
    const status = this.extractStatus(e);
    if (status === 404) return 'URL não encontrada';
    return 'Erro ao consultar estatísticas';
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