import { TestBed } from '@angular/core/testing';
import { ShortenAdapter } from 'app/features/shorten/data/shorten.adapter';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('ShortenAdapter (unit)', () => {
  let adapter: ShortenAdapter;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    adapter = TestBed.inject(ShortenAdapter);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve mapear a resposta para domínio', async () => {
    const promise = adapter.create({ url: 'https://example.com', code: 'abc12' });

    const req = httpMock.expectOne('/api/shorten');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, code: 'abc12', originalUrl: 'https://example.com', createdAt: '2025-01-01T00:00:00Z' });

    await expect(promise).resolves.toEqual({ id: 1, code: 'abc12', originalUrl: 'https://example.com', createdAt: '2025-01-01T00:00:00Z' });
  });

  it('deve lançar erro quando resposta vazia', async () => {
    const promise = adapter.create({ url: 'https://example.com' });

    const req = httpMock.expectOne('/api/shorten');
    expect(req.request.method).toBe('POST');
    req.flush(null);

    await expect(promise).rejects.toThrow('Resposta vazia');
  });
});