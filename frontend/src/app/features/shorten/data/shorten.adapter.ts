import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import type { components } from 'app/core/api/types';

// Mantém o adapter isolado para conversões DTO <-> Domain
export type ShortUrlDTO = components['schemas']['ShortenResponse'];
export type ShortUrlDomain = Readonly<{
  id: number;
  code: string;
  originalUrl: string;
  createdAt: string;
}>;

@Injectable({ providedIn: 'root' })
export class ShortenAdapter {
  private readonly http = inject(HttpClient);

  async create(dto: components['schemas']['ShortenRequest']): Promise<ShortUrlDomain> {
    const res = await firstValueFrom(this.http.post<ShortUrlDTO>('/shorten', dto));
    if (!res) throw new Error('Resposta vazia');
    return this.toDomain(res);
  }

  toDomain(dto: ShortUrlDTO): ShortUrlDomain {
    const id = dto.id;
    const code = dto.code;
    const originalUrl = dto.originalUrl;
    const createdAt = dto.createdAt;

    if (
      typeof id !== 'number' ||
      typeof code !== 'string' ||
      typeof originalUrl !== 'string' ||
      typeof createdAt !== 'string'
    ) {
      throw new Error('Resposta inválida do servidor');
    }

    return { id, code, originalUrl, createdAt } as const;
  }
}