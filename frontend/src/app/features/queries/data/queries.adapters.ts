import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import type { components } from 'app/core/api/types';

export type StatsDTO = components['schemas']['StatsResponse'];
export type RankingItemDTO = components['schemas']['RankingItem'];

// Tipagem de paginação compatível com Spring Data
export type Page<T> = Readonly<{
  content: ReadonlyArray<T>;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // página atual (0-based)
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}>;

export type StatsDomain = Readonly<{
  code: string;
  originalUrl: string;
  hits: number;
}>;

export type StatsPageDomain = Page<StatsDomain>;

export type RankingDomainItem = Readonly<{
  code: string;
  hits: number;
}>;

@Injectable({ providedIn: 'root' })
export class StatsAdapter {
  private readonly http = inject(HttpClient);

  async getByCode(code: string): Promise<StatsDomain> {
    const res = await firstValueFrom(this.http.get<StatsDTO>(`/api/stats/${encodeURIComponent(code)}`));
    if (!res) throw new Error('Resposta vazia');
    return this.toDomain(res);
  }

  async list(page: number, size: number): Promise<StatsPageDomain> {
    const dto = await firstValueFrom(
      this.http.get<{
        content: StatsDTO[];
        totalElements: number;
        totalPages: number;
        size: number;
        number: number;
        first: boolean;
        last: boolean;
        numberOfElements: number;
        empty: boolean;
      }>(`/api/stats?page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`)
    );

    const content = Array.isArray(dto.content) ? dto.content.map(this.toDomain) : [];
    return {
      content,
      totalElements: dto.totalElements,
      totalPages: dto.totalPages,
      size: dto.size,
      number: dto.number,
      first: dto.first,
      last: dto.last,
      numberOfElements: dto.numberOfElements,
      empty: dto.empty,
    } as const;
  }

  toDomain(dto: StatsDTO): StatsDomain {
    const code = dto.code;
    const originalUrl = dto.originalUrl;
    const hits = dto.hits;

    if (typeof code !== 'string' || typeof originalUrl !== 'string' || typeof hits !== 'number') {
      throw new Error('Resposta inválida do servidor');
    }
    return { code, originalUrl, hits } as const;
  }
}

@Injectable({ providedIn: 'root' })
export class RankingAdapter {
  private readonly http = inject(HttpClient);

  async list(): Promise<ReadonlyArray<RankingDomainItem>> {
    const res = await firstValueFrom(this.http.get<RankingItemDTO[] | RankingItemDTO>(`/api/ranking`));
    const arr: RankingItemDTO[] = Array.isArray(res) ? res : res ? [res] : [];
    return arr.map(this.toDomain);
  }

  private readonly toDomain = (dto: RankingItemDTO): RankingDomainItem => {
    const code = dto.code;
    const hits = dto.hits;
    if (typeof code !== 'string' || typeof hits !== 'number') {
      throw new Error('Resposta inválida do servidor');
    }
    return { code, hits } as const;
  };
}