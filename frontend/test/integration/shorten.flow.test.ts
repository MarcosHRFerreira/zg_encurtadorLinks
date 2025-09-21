import { TestBed } from '@angular/core/testing';
import ShortenPageComponent from 'app/features/shorten/pages/shorten.page';
import { provideRouter, Routes } from '@angular/router';
import { App } from 'app/app';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

// Teste de integração focado no fluxo de POST /shorten usando HttpTestingController

describe('Shorten Flow (integration)', () => {
  const routes: Routes = [
    { path: '', component: App },
    { path: 'shorten', loadComponent: () => Promise.resolve(ShortenPageComponent) },
  ];

  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [provideRouter(routes)],
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve renderizar e submeter com sucesso (200)', async () => {
    const fixture = TestBed.createComponent(ShortenPageComponent);
    const comp = fixture.componentInstance;

    comp.form.setValue({ url: 'https://example.com', code: 'abc12' });

    const submitPromise = comp.onSubmit();

    const req = httpMock.expectOne('/shorten');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ url: 'https://example.com', code: 'abc12' });

    req.flush({ id: 1, code: 'abc12', originalUrl: 'https://example.com', createdAt: '2025-01-01T00:00:00Z' });

    await submitPromise;

    expect(comp.facade.error()).toBeNull();
    expect(comp.facade.result()).toEqual({ id: 1, code: 'abc12', originalUrl: 'https://example.com', createdAt: '2025-01-01T00:00:00Z' });
  });

  it('deve lidar com erro 400 (alternativo)', async () => {
    const fixture = TestBed.createComponent(ShortenPageComponent);
    const comp = fixture.componentInstance;

    comp.form.setValue({ url: 'https://invalid', code: '' });

    const submitPromise = comp.onSubmit();

    const req = httpMock.expectOne('/shorten');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ url: 'https://invalid', code: '' });

    req.flush({ message: 'invalid' }, { status: 400, statusText: 'Bad Request' });

    await submitPromise;

    expect(comp.facade.result()).toBeNull();
    expect(comp.facade.error()).toBe('URL inválida');
  });
});