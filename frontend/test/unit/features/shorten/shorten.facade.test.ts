import { TestBed } from '@angular/core/testing';
import sinon from 'sinon';
import { ShortenFacade } from 'app/features/shorten/state/shorten.facade';
import { ShortenAdapter, type ShortUrlDomain } from 'app/features/shorten/data/shorten.adapter';
import type { components } from 'app/core/api/types';

type ShortenRequest = components['schemas']['ShortenRequest'];

describe('ShortenFacade (unit)', () => {
  let facade: ShortenFacade;
  let adapterStub: Pick<ShortenAdapter, 'create'>;

  const validRequest: ShortenRequest = { url: 'https://example.com', code: 'ABCDE' } as const;
  const domain: ShortUrlDomain = { id: 1, code: 'ABCDE', originalUrl: 'https://example.com', createdAt: '2024-01-01T00:00:00Z' } as const;

  beforeEach(() => {
    adapterStub = { create: sinon.stub() } as unknown as Pick<ShortenAdapter, 'create'>;

    TestBed.configureTestingModule({
      providers: [
        ShortenFacade,
        { provide: ShortenAdapter, useValue: adapterStub },
      ],
    });

    facade = TestBed.inject(ShortenFacade);
  });

  it('deve resetar estados corretamente', () => {
    facade.reset();
    expect(facade.loading()).toBe(false);
    expect(facade.error()).toBeNull();
    expect(facade.result()).toBeNull();
  });

  it('deve setar resultado ao criar com sucesso', async () => {
    (adapterStub.create as sinon.SinonStub).resolves(domain);

    await facade.create(validRequest);

    expect(facade.error()).toBeNull();
    expect(facade.result()).toEqual(domain);
    expect(facade.loading()).toBe(false);
  });

  it('deve mapear erro 400 para "URL inválida"', async () => {
    (adapterStub.create as sinon.SinonStub).rejects({ status: 400 });

    await facade.create(validRequest);

    expect(facade.error()).toBe('URL inválida');
    expect(facade.result()).toBeNull();
    expect(facade.loading()).toBe(false);
  });

  it('deve mapear erro 409 para "Código já utilizado"', async () => {
    (adapterStub.create as sinon.SinonStub).rejects({ status: 409 });

    await facade.create(validRequest);

    expect(facade.error()).toBe('Código já utilizado');
    expect(facade.result()).toBeNull();
    expect(facade.loading()).toBe(false);
  });

  it('deve mapear erro genérico para "Erro ao encurtar URL"', async () => {
    (adapterStub.create as sinon.SinonStub).rejects({ status: 500 });

    await facade.create(validRequest);

    expect(facade.error()).toBe('Erro ao encurtar URL');
    expect(facade.result()).toBeNull();
    expect(facade.loading()).toBe(false);
  });

  it('deve suportar diferentes formatos de erro em extractStatus', async () => {
    const errors: Array<unknown> = [
      { status: 400 },
      { statusCode: 409 },
      { response: { status: 400 } },
      { cause: { status: 409 } },
      { error: { status: 500 } },
    ];

    for (const err of errors) {
      (adapterStub.create as sinon.SinonStub).rejects(err);
      await facade.create(validRequest);
    }

    expect(facade.loading()).toBe(false);
  });
});