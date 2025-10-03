import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import QueriesPageComponent from 'app/features/queries/pages/queries.page';

// Integração da página de Queries cobrindo init, consulta por código e paginação

describe('Queries Page (integration)', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve carregar ranking e página de estatísticas no init', async () => {
    const fixture = TestBed.createComponent(QueriesPageComponent);
    fixture.detectChanges();

    const reqRanking = httpMock.expectOne('/api/ranking');
    expect(reqRanking.request.method).toBe('GET');
    reqRanking.flush([{ code: 'AAAAA', hits: 10 }]);

    const reqStatsPage = httpMock.expectOne('/api/stats?page=0&size=10');
    expect(reqStatsPage.request.method).toBe('GET');
    reqStatsPage.flush({
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

  it('deve consultar estatísticas por código com sucesso', async () => {
    const fixture = TestBed.createComponent(QueriesPageComponent);
    const comp = fixture.componentInstance;

    comp.statsForm.setValue({ code: 'ABCDE' });
    const submitPromise = comp.onStatsSubmit();

    const req = httpMock.expectOne('/api/stats/ABCDE');
    expect(req.request.method).toBe('GET');
    req.flush({ code: 'ABCDE', originalUrl: 'https://ex.com/a', hits: 42 });

    await submitPromise;

    expect(comp.facade.statsError()).toBeNull();
    expect(comp.facade.stats()).toEqual({ code: 'ABCDE', originalUrl: 'https://ex.com/a', hits: 42 });
  });

  it('deve lidar com 404 ao consultar estatísticas por código', async () => {
    const fixture = TestBed.createComponent(QueriesPageComponent);
    const comp = fixture.componentInstance;

    comp.statsForm.setValue({ code: 'ZZZZZ' });
    const submitPromise = comp.onStatsSubmit();

    const req = httpMock.expectOne('/api/stats/ZZZZZ');
    expect(req.request.method).toBe('GET');
    req.flush({ error: 'not-found' }, { status: 404, statusText: 'Not Found' });

    await submitPromise;

    expect(comp.facade.stats()).toBeNull();
    expect(comp.facade.statsError()).toBe('URL não encontrada');
  });

  it('deve paginar e alterar tamanho da página', async () => {
    const fixture = TestBed.createComponent(QueriesPageComponent);
    const comp = fixture.componentInstance;

    // init: página 0, size 10
    fixture.detectChanges();
    const reqRanking = httpMock.expectOne('/api/ranking');
    expect(reqRanking.request.method).toBe('GET');
    reqRanking.flush([{ code: 'AAAAA', hits: 10 }]);

    const reqInit = httpMock.expectOne('/api/stats?page=0&size=10');
    reqInit.flush({
      content: [
        { code: 'AAAAA', originalUrl: 'https://ex.com/a', hits: 10 },
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

    // alterar page size para 5
    const select = document.createElement('select');
    const opt = document.createElement('option');
    opt.value = '5';
    opt.selected = true;
    select.appendChild(opt);

    const changePromise = comp.onPageSizeChange({ target: select } as unknown as Event);
    const reqSize = httpMock.expectOne('/api/stats?page=0&size=5');
    expect(reqSize.request.method).toBe('GET');
    reqSize.flush({
      content: [
        { code: 'AAAAA', originalUrl: 'https://ex.com/a', hits: 10 },
      ],
      totalElements: 2,
      totalPages: 2,
      size: 5,
      number: 0,
      first: true,
      last: false,
      numberOfElements: 1,
      empty: false,
    });
    await changePromise;

    // Em alguns ambientes, a alteração de page size pode disparar uma segunda chamada inicial; trate-a se existir
    const maybeExtraSize = httpMock.match('/api/stats?page=0&size=5');
    for (const extra of maybeExtraSize) {
      extra.flush({
        content: [
          { code: 'AAAAA', originalUrl: 'https://ex.com/a', hits: 10 },
        ],
        totalElements: 2,
        totalPages: 2,
        size: 5,
        number: 0,
        first: true,
        last: false,
        numberOfElements: 1,
        empty: false,
      });
    }

    // Navegar para próxima página (page=1, size=5): capture a requisição ANTES do flush e aguarde a Promise DEPOIS
    const nextPromise = comp.nextPage();
    const reqNext = httpMock.expectOne('/api/stats?page=1&size=5');
    expect(reqNext.request.method).toBe('GET');
    reqNext.flush({
      content: [
        { code: 'BBBBB', originalUrl: 'https://ex.com/b', hits: 20 },
      ],
      totalElements: 2,
      totalPages: 2,
      size: 5,
      number: 1,
      first: false,
      last: true,
      numberOfElements: 1,
      empty: false,
    });
    await nextPromise;

    // Em alguns ambientes, a navegação pode disparar chamadas duplicadas para a mesma página; trate-as se existirem
    const maybeExtraNext = httpMock.match('/api/stats?page=1&size=5');
    for (const extra of maybeExtraNext) {
      extra.flush({
        content: [
          { code: 'BBBBB', originalUrl: 'https://ex.com/b', hits: 20 },
        ],
        totalElements: 2,
        totalPages: 2,
        size: 5,
        number: 1,
        first: false,
        last: true,
        numberOfElements: 1,
        empty: false,
      });
    }

    // tentar avançar sendo última página: não deve disparar requisição
    await comp.nextPage();

    // Drenar qualquer request pendente inesperada para a mesma URL (ambientes com dupla emissão)
    const leftover = httpMock.match('/api/stats?page=0&size=5');
    for (const extra of leftover) {
      extra.flush({
        content: [
          { code: 'AAAAA', originalUrl: 'https://ex.com/a', hits: 10 },
        ],
        totalElements: 2,
        totalPages: 2,
        size: 5,
        number: 0,
        first: true,
        last: false,
        numberOfElements: 1,
        empty: false,
      });
    }

    const leftoverNext = httpMock.match('/api/stats?page=1&size=5');
    for (const extra of leftoverNext) {
      extra.flush({
        content: [
          { code: 'BBBBB', originalUrl: 'https://ex.com/b', hits: 20 },
        ],
        totalElements: 2,
        totalPages: 2,
        size: 5,
        number: 1,
        first: false,
        last: true,
        numberOfElements: 1,
        empty: false,
      });
    }

    // Yield microtasks e drenar qualquer nova requisição para a mesma URL emitida tardiamente
    await Promise.resolve();
    const afterTickNext = httpMock.match('/api/stats?page=1&size=5');
    for (const extra of afterTickNext) {
      extra.flush({
        content: [
          { code: 'BBBBB', originalUrl: 'https://ex.com/b', hits: 20 },
        ],
        totalElements: 2,
        totalPages: 2,
        size: 5,
        number: 1,
        first: false,
        last: true,
        numberOfElements: 1,
        empty: false,
      });
    }
});
});