import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import sinon from 'sinon';
import { StatsAdapter, RankingAdapter } from 'app/features/queries/data/queries.adapters';
import { QueriesFacade } from 'app/features/queries/state/queries.facade';

// Pequena tipagem para facilitar criação do stub do HttpClient
type HttpClientStub = Pick<HttpClient, 'get'>;

function makeHttpStub<T>(value: T | Error): HttpClientStub {
  const get = sinon.stub().callsFake((_url: string) => {
    if (value instanceof Error) return throwError(() => value);
    return of(value as T);
  });
  return { get } as HttpClientStub;
}

describe('Integração: fluxo de estatísticas (Consultas)', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('deve carregar estatísticas por code com sucesso', async () => {
    // Arrange
    const dto = { code: 'abc', originalUrl: 'https://site.com', hits: 10 };
    const http = makeHttpStub(dto);
    TestBed.configureTestingModule({
      providers: [
        QueriesFacade,
        StatsAdapter,
        RankingAdapter,
        { provide: HttpClient, useValue: http },
      ],
    });
    const facade = TestBed.inject(QueriesFacade);

    // Act
    await facade.fetchStats('abc');

    // Assert
    const stats = facade.stats();
    expect(stats).not.toBeNull();
    expect(stats!.code).toBe('abc');
    expect(stats!.hits).toBe(10);
    expect(facade.statsError()).toBeNull();
    expect(facade.statsLoading()).toBe(false);
  });

  it('deve sinalizar 404 com mensagem "URL não encontrada"', async () => {
    // Arrange
    const err = new Error('not found') as Error & { status?: number };
    err.status = 404;
    const http = makeHttpStub(err);
    TestBed.configureTestingModule({
      providers: [
        QueriesFacade,
        StatsAdapter,
        RankingAdapter,
        { provide: HttpClient, useValue: http },
      ],
    });
    const facade = TestBed.inject(QueriesFacade);

    // Act
    await facade.fetchStats('missing');

    // Assert
    expect(facade.stats()).toBeNull();
    expect(facade.statsError()).toBe('URL não encontrada');
    expect(facade.statsLoading()).toBe(false);
  });

  it('deve carregar página de estatísticas e paginar corretamente', async () => {
    // Arrange
    const pageDto = {
      content: [
        { code: 'a', originalUrl: 'https://a.com', hits: 1 },
        { code: 'b', originalUrl: 'https://b.com', hits: 2 },
      ],
      totalElements: 2,
      totalPages: 1,
      size: 2,
      number: 0,
      first: true,
      last: true,
      numberOfElements: 2,
      empty: false,
    };
    const pageDtoSmall = {
      ...pageDto,
      content: [{ code: 'a', originalUrl: 'https://a.com', hits: 1 }],
      size: 1,
      numberOfElements: 1,
    };
    const get = sinon.stub();
    get.onFirstCall().callsFake((_url: string) => of(pageDto));
    get.onSecondCall().callsFake((_url: string) => of(pageDtoSmall));
    const http: HttpClientStub = { get } as HttpClientStub;
    TestBed.configureTestingModule({
      providers: [
        QueriesFacade,
        StatsAdapter,
        RankingAdapter,
        { provide: HttpClient, useValue: http },
      ],
    });
    const facade = TestBed.inject(QueriesFacade);

    // Act
    await facade.fetchStatsPage(0, 2);

    // Assert
    const page = facade.statsPage();
    expect(page).not.toBeNull();
    expect(page!.content.length).toBe(2);
    expect(facade.statsPageError()).toBeNull();
    expect(facade.statsPageLoading()).toBe(false);

    // Testar mudança de page size
    await facade.setStatsPageSize(1);
    expect(facade.statsPageNumber()).toBe(0);
    expect(facade.statsPageSize()).toBe(1);
  });

  it('deve sinalizar erro genérico na página', async () => {
    // Arrange
    const http = makeHttpStub(new Error('server down'));
    TestBed.configureTestingModule({
      providers: [
        QueriesFacade,
        StatsAdapter,
        RankingAdapter,
        { provide: HttpClient, useValue: http },
      ],
    });
    const facade = TestBed.inject(QueriesFacade);

    // Act
    await facade.fetchStatsPage(0, 10);

    // Assert
    expect(facade.statsPage()).toBeNull();
    expect(facade.statsPageError()).toBe('Erro ao carregar estatísticas paginadas');
    expect(facade.statsPageLoading()).toBe(false);
  });
});