import { TestBed } from '@angular/core/testing';
import sinon from 'sinon';
import { QueriesFacade } from 'app/features/queries/state/queries.facade';
import { StatsAdapter, RankingAdapter } from 'app/features/queries/data/queries.adapters';

// Unit tests para QueriesFacade cobrindo mapeamentos de erro e guardas de paginação

type StatsAdapterPort = Pick<StatsAdapter, 'getByCode' | 'list'>;
type RankingAdapterPort = Pick<RankingAdapter, 'list'>;

describe('QueriesFacade (unit)', () => {
  let facade: QueriesFacade;
  let statsStub: StatsAdapterPort;
  let rankingStub: RankingAdapterPort;

  beforeEach(() => {
    statsStub = {
      getByCode: sinon.stub(),
      list: sinon.stub(),
    } as unknown as StatsAdapterPort;

    rankingStub = {
      list: sinon.stub(),
    } as unknown as RankingAdapterPort;

    TestBed.configureTestingModule({
      providers: [
        QueriesFacade,
        { provide: StatsAdapter, useValue: statsStub },
        { provide: RankingAdapter, useValue: rankingStub },
      ],
    });

    facade = TestBed.inject(QueriesFacade);
  });

  it('deve mapear erro 404 para "URL não encontrada"', async () => {
    (statsStub.getByCode as sinon.SinonStub).rejects({ status: 404 });
    await facade.fetchStats('ABCDE');
    expect(facade.statsError()).toBe('URL não encontrada');
    expect(facade.stats()).toBeNull();
  });

  it('deve mapear erro genérico para "Erro ao consultar estatísticas"', async () => {
    (statsStub.getByCode as sinon.SinonStub).rejects({ status: 500 });
    await facade.fetchStats('ABCDE');
    expect(facade.statsError()).toBe('Erro ao consultar estatísticas');
    expect(facade.stats()).toBeNull();
  });

  it('prevStatsPage não deve buscar quando for primeira página', async () => {
    (statsStub.list as sinon.SinonStub).resolves({
      content: [], totalElements: 0, totalPages: 1, size: 10, number: 0, first: true, last: false, numberOfElements: 0, empty: true,
    });
    await facade.fetchStatsPage(0, 10);
    (statsStub.list as sinon.SinonStub).resetHistory();
    await facade.prevStatsPage();
    expect((statsStub.list as sinon.SinonStub).callCount).toBe(0);
  });

  it('nextStatsPage não deve buscar quando for última página', async () => {
    (statsStub.list as sinon.SinonStub).resolves({
      content: [], totalElements: 1, totalPages: 1, size: 10, number: 0, first: false, last: true, numberOfElements: 1, empty: false,
    });
    await facade.fetchStatsPage(0, 10);
    (statsStub.list as sinon.SinonStub).resetHistory();
    await facade.nextStatsPage();
    expect((statsStub.list as sinon.SinonStub).callCount).toBe(0);
  });

  it('nextStatsPage deve buscar próxima página quando não for última', async () => {
    (statsStub.list as sinon.SinonStub).resolves({
      content: [], totalElements: 2, totalPages: 2, size: 10, number: 0, first: true, last: false, numberOfElements: 1, empty: false,
    });
    await facade.fetchStatsPage(0, 10);
    (statsStub.list as sinon.SinonStub).resetHistory();
    await facade.nextStatsPage();
    expect((statsStub.list as sinon.SinonStub).callCount).toBe(1);
    const args = (statsStub.list as sinon.SinonStub).firstCall.args;
    expect(args[0]).toBe(1);
    expect(args[1]).toBe(10);
  });

  it('setStatsPageSize deve sanitizar valor e resetar para página 0', async () => {
    (statsStub.list as sinon.SinonStub).resolves({
      content: [], totalElements: 0, totalPages: 0, size: 1, number: 0, first: true, last: true, numberOfElements: 0, empty: true,
    });
    await facade.setStatsPageSize(0);
    expect(facade.statsPageSize()).toBe(1);
    const args = (statsStub.list as sinon.SinonStub).firstCall.args;
    expect(args[0]).toBe(0);
    expect(args[1]).toBe(1);
  });

  it('fetchRanking deve setar erro ao falhar', async () => {
    (rankingStub.list as sinon.SinonStub).rejects(new Error('falha'));
    await facade.fetchRanking();
    expect(facade.rankingError()).toBe('Erro ao carregar ranking');
    expect(facade.ranking()).toEqual([]);
  });

  it('fetchStatsPage deve setar erro ao falhar', async () => {
    (statsStub.list as sinon.SinonStub).rejects(new Error('falha'));
    await facade.fetchStatsPage(0, 10);
    expect(facade.statsPageError()).toBe('Erro ao carregar estatísticas paginadas');
    expect(facade.statsPage()).toBeNull();
  });
});