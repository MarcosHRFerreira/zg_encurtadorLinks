import { TestBed } from '@angular/core/testing';
import { ShortenFacade } from 'app/features/shorten/state/shorten.facade';
import { ShortenAdapter, type ShortUrlDomain } from 'app/features/shorten/data/shorten.adapter';

type ShortenAdapterPort = Pick<ShortenAdapter, 'create'>;

const successResult: ShortUrlDomain = {
  id: 1,
  code: 'abc12',
  originalUrl: 'https://example.com',
  createdAt: '2025-01-01T00:00:00Z',
} as const;

describe('ShortenFacade (unit)', () => {
  let facade: ShortenFacade;
  let adapterStub: ShortenAdapterPort;

  beforeEach(() => {
    adapterStub = {
      create: jest.fn(async () => successResult),
    };

    TestBed.configureTestingModule({
      providers: [
        ShortenFacade,
        { provide: ShortenAdapter, useValue: adapterStub },
      ],
    });

    facade = TestBed.inject(ShortenFacade);
  });

  it('deve atualizar estado com resultado após sucesso', async () => {
    await facade.create({ url: 'https://example.com', code: 'abc12' } as { url: string; code?: string });

    expect(facade.error()).toBeNull();
    expect(facade.result()).toEqual(successResult);
    expect(facade.loading()).toBe(false);
    expect((adapterStub.create as jest.Mock).mock.calls.length).toBe(1);
  });

  it('deve setar erro ao falhar', async () => {
    (adapterStub.create as jest.Mock).mockRejectedValueOnce({ status: 400 });

    await facade.create({ url: 'foo' } as { url: string });

    expect((adapterStub.create as jest.Mock).mock.calls.length).toBe(1);
    expect(facade.result()).toBeNull();
    expect(facade.error()).toBe('URL inválida');
  });
});