import { Injectable, computed, inject, signal } from '@angular/core';
import { StatsAdapter, type StatsPageDomain, RankingAdapter, type RankingDomainItem, type StatsCodeSummaryDomain } from '../data/queries.adapters';

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
  private readonly _stats = signal<StatsCodeSummaryDomain | null>(null);

  // Paginação de estatísticas
  private readonly _statsPageLoading = signal(false);
  private readonly _statsPageError = signal<string | null>(null);
  private readonly _statsPage = signal<StatsPageDomain | null>(null);
  private readonly _statsPageNumber = signal(0);
  private readonly _statsPageSize = signal(10);

  private readonly _rankingLoading = signal(false);
  private readonly _rankingError = signal<string | null>(null);
  private readonly _ranking = signal<ReadonlyArray<RankingDomainItem>>([]);

  // (Resumo geral removido)

  readonly statsLoading = computed(() => this._statsLoading());
  readonly statsError = computed(() => this._statsError());
  readonly stats = computed(() => this._stats());

  // Computeds de paginação de estatísticas
  readonly statsPageLoading = computed(() => this._statsPageLoading());
  readonly statsPageError = computed(() => this._statsPageError());
  readonly statsPage = computed(() => this._statsPage());
  readonly statsPageNumber = computed(() => this._statsPageNumber());
  readonly statsPageSize = computed(() => this._statsPageSize());

  readonly rankingLoading = computed(() => this._rankingLoading());
  readonly rankingError = computed(() => this._rankingError());
  readonly ranking = computed(() => this._ranking());

  // (Resumo geral removido)

  resetStats(): void {
    this._statsLoading.set(false);
    this._statsError.set(null);
    this._stats.set(null);
  }

  resetStatsPage(): void {
    this._statsPageLoading.set(false);
    this._statsPageError.set(null);
    this._statsPage.set(null);
    this._statsPageNumber.set(0);
  }

  resetRanking(): void {
    this._rankingLoading.set(false);
    this._rankingError.set(null);
    this._ranking.set([]);
  }

  // (Resumo geral removido)

  async fetchStats(code: string): Promise<void> {
    this._statsLoading.set(true);
    this._statsError.set(null);
    this._stats.set(null);
    try {
      const res = await this.statsAdapter.getCodeSummary(code);
      this._stats.set(res);
    } catch (e: unknown) {
      const message = this.mapStatsError(e as ErrorLike);
      this._statsError.set(message);
    } finally {
      this._statsLoading.set(false);
    }
  }

  // (Resumo geral removido)

  async fetchStatsPage(page?: number, size?: number): Promise<void> {
    const pageNumber = typeof page === 'number' ? page : this._statsPageNumber();
    const pageSize = typeof size === 'number' ? size : this._statsPageSize();

    this._statsPageLoading.set(true);
    this._statsPageError.set(null);
    this._statsPage.set(null);
    try {
      const res = await this.statsAdapter.list(pageNumber, pageSize);
      this._statsPage.set(res);
      this._statsPageNumber.set(res.number);
      this._statsPageSize.set(res.size);
    } catch (_e: unknown) {
      this._statsPageError.set('Erro ao carregar estatísticas paginadas');
    } finally {
      this._statsPageLoading.set(false);
    }
  }

  nextStatsPage(): Promise<void> {
    const p = this._statsPage();
    if (p?.last) return Promise.resolve();
    return this.fetchStatsPage(this._statsPageNumber() + 1);
  }

  prevStatsPage(): Promise<void> {
    const p = this._statsPage();
    if (p?.first) return Promise.resolve();
    return this.fetchStatsPage(this._statsPageNumber() - 1);
  }

  setStatsPageSize(size: number): Promise<void> {
    if (!Number.isFinite(size)) {
      // ignora evento inválido sem disparar requisição
      return Promise.resolve();
    }
    const sanitized = Math.max(1, Math.floor(size));
    this._statsPageSize.set(sanitized);
    return this.fetchStatsPage(0, sanitized);
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