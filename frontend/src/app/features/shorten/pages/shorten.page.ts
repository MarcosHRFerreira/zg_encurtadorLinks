import { Component, ChangeDetectionStrategy, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { formatDateBrLocal } from 'app/core/utils/date-format';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ShortenFacade } from '../state/shorten.facade';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'app-shorten-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButton],
  template: `
    <section class="container">
      <h2>Encurtar URL</h2>
      <form [formGroup]="form" (submit)="$event.preventDefault()" (ngSubmit)="onSubmit($event)" method="post" novalidate>
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>URL</mat-label>
          <input matInput type="url" formControlName="url" placeholder="https://..." required />
        </mat-form-field>
        <div class="error" *ngIf="form.controls.url.invalid && form.controls.url.touched">
          Informe uma URL válida
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Código (opcional)</mat-label>
          <input matInput type="text" formControlName="code" maxlength="5" pattern="^[A-Za-z0-9]{5}$" />
        </mat-form-field>

        <button [matButton]="'filled'" class="rounded-full" type="submit" [disabled]="facade.loading() || form.invalid">Encurtar</button>
      </form>

      <div class="result" *ngIf="facade.result() as r">
        <p>Seu link:
          <strong>
            <a [href]="(r.shortUrl ?? (backendOrigin + '/' + r.code))" target="_blank" rel="noopener">
              {{ r.shortUrl ?? (backendOrigin + '/' + r.code) }}
            </a>
          </strong>
        </p>
        <p>Original: <a [href]="r.originalUrl" target="_blank" rel="noopener">{{ r.originalUrl }}</a></p>
        <p>Criado em: {{ formatDate(r.createdAt) }}</p>
      </div>

      <div class="error" *ngIf="facade.error() as err">{{ err }}</div>
    </section>
  `,
  styles: [
    `.container{max-width:640px;margin:24px auto;padding:16px}`,
    `.container h2{font-size:24px;font-weight:700;margin-bottom:16px;border-left:6px solid #3f51b5;padding-left:10px}`,
    `.btn{padding:8px 12px;margin-top:8px}`,
    `.error{color:#c0392b;margin-top:8px}`,
    `.result{background:#f6f8fa;padding:12px;margin-top:12px;border-radius:6px}`
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export default class ShortenPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly facade = inject(ShortenFacade);

  readonly backendOrigin: string = typeof window !== 'undefined' && window.__ENV__?.API_BASE_URL
    ? String(window.__ENV__?.API_BASE_URL).replace(/\/$/, '')
    : (typeof window !== 'undefined'
        ? `${window.location.protocol}//${window.location.hostname}:8080`
        : 'http://localhost:8080');

  readonly form = this.fb.nonNullable.group({
    url: ['', [Validators.required, Validators.pattern(/https?:\/\/.+/)]],
    code: ['']
  });

  async onSubmit(event?: Event): Promise<void> {
    if (event) event.preventDefault();
    if (this.form.invalid) return;
    const { url, code } = this.form.getRawValue();
    const payload: { url: string; code?: string } = { url };
    if (code && code.trim().length > 0) payload.code = code;
    await this.facade.create(payload as { url: string; code?: string });
  }

  ngOnInit(): void {
    // Limpa estado e formulário ao entrar na tela de Encurtar
    this.facade.reset();
    this.form.reset({ url: '', code: '' });
  }

  formatDate(value: string): string {
    return formatDateBrLocal(value);
  }
}