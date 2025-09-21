import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import type { components } from 'app/core/api/types';

export type StatsDTO = components['schemas']['StatsResponse'];
export type RankingItemDTO = components['schemas']['RankingItem'];

export type StatsDomain = Readonly<{
  code: string;
  originalUrl: string;
  hits: number;
}>;

export type RankingDomainItem = Readonly<{
  code: string;
  hits: number;
}>;

@Injectable({ providedIn: 'root' })
export class StatsAdapter {
  private readonly http = inject(HttpClient);

  async getByCode(code: string): Promise<StatsDomain> {
    const res = await firstValueFrom(this.http.get<StatsDTO>(`/stats/${encodeURIComponent(code)}`));
    if (!res) throw new Error('Resposta vazia');
    return this.toDomain(res);
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
    const res = await firstValueFrom(this.http.get<RankingItemDTO[] | RankingItemDTO>(`/ranking`));
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