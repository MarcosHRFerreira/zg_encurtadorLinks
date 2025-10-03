import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { StatsAdapter, RankingAdapter } from 'app/features/queries/data/queries.adapters';

// Unit tests para Adapters de Queries cobrindo mapeamentos e casos alternativos

describe('Queries Adapters (unit)', () => {
  let httpMock: HttpTestingController;
  let stats: StatsAdapter;
  let ranking: RankingAdapter;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [StatsAdapter, RankingAdapter],
    });
    httpMock = TestBed.inject(HttpTestingController);
    stats = TestBed.inject(StatsAdapter);
    ranking = TestBed.inject(RankingAdapter);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('StatsAdapter.getByCode deve mapear sucesso', async () => {
    const promise = stats.getByCode('ABCDE');
    const req = httpMock.expectOne('/api/stats/ABCDE');
    expect(req.request.method).toBe('GET');
    req.flush({ code: 'ABCDE', originalUrl: 'https://ex.com/a', hits: 42 });
    await expect(promise).resolves.toEqual({ code: 'ABCDE', originalUrl: 'https://ex.com/a', hits: 42 });
  });

  it('StatsAdapter.getByCode deve lançar quando resposta vazia', async () => {
    const promise = stats.getByCode('XXXXX');
    const req = httpMock.expectOne('/api/stats/XXXXX');
    expect(req.request.method).toBe('GET');
    req.flush(null);
    await expect(promise).rejects.toThrow('Resposta vazia');
  });

  it('StatsAdapter.getByCode deve lançar quando tipos inválidos', async () => {
    const promise = stats.getByCode('ABCDE');
    const req = httpMock.expectOne('/api/stats/ABCDE');
    req.flush({ code: 'ABCDE', originalUrl: 123, hits: 42 });
    await expect(promise).rejects.toThrow('Resposta inválida do servidor');
  });

  it('StatsAdapter.list deve mapear página', async () => {
    const promise = stats.list(0, 10);
    const req = httpMock.expectOne('/api/stats?page=0&size=10');
    expect(req.request.method).toBe('GET');
    req.flush({
      content: [
        { code: 'AAAAA', originalUrl: 'https://ex.com/a', hits: 10 },
        { code: 'BBBBB', originalUrl: 'https://ex.com/b', hits: 20 },
      ],
      totalElements: 2,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      numberOfElements: 2,
      empty: false,
    });
    await expect(promise).resolves.toEqual({
      content: [
        { code: 'AAAAA', originalUrl: 'https://ex.com/a', hits: 10 },
        { code: 'BBBBB', originalUrl: 'https://ex.com/b', hits: 20 },
      ],
      totalElements: 2,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      numberOfElements: 2,
      empty: false,
    });
  });

  it('StatsAdapter.list deve lançar quando item inválido', async () => {
    const promise = stats.list(0, 10);
    const req = httpMock.expectOne('/api/stats?page=0&size=10');
    req.flush({
      content: [
        { code: 'AAAAA', hits: 10 },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      numberOfElements: 1,
      empty: false,
    });
    await expect(promise).rejects.toThrow('Resposta inválida do servidor');
  });

  it('RankingAdapter.list deve mapear array', async () => {
    const promise = ranking.list();
    const req = httpMock.expectOne('/api/ranking');
    expect(req.request.method).toBe('GET');
    req.flush([
      { code: 'AAAAA', hits: 10 },
      { code: 'BBBBB', hits: 20 },
    ]);
    await expect(promise).resolves.toEqual([
      { code: 'AAAAA', hits: 10 },
      { code: 'BBBBB', hits: 20 },
    ]);
  });

  it('RankingAdapter.list deve mapear objeto único', async () => {
    const promise = ranking.list();
    const req = httpMock.expectOne('/api/ranking');
    req.flush({ code: 'AAAAA', hits: 10 });
    await expect(promise).resolves.toEqual([{ code: 'AAAAA', hits: 10 }]);
  });

  it('RankingAdapter.list deve lançar quando tipos inválidos', async () => {
    const promise = ranking.list();
    const req = httpMock.expectOne('/api/ranking');
    req.flush({ code: 'AAAAA', hits: 'x' });
    await expect(promise).rejects.toThrow('Resposta inválida do servidor');
  });
});