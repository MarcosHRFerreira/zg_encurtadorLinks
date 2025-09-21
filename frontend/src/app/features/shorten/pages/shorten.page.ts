import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ShortenFacade } from '../state/shorten.facade';

@Component({
  selector: 'app-shorten-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="container">
      <h2>Encurtar URL</h2>
      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <label>URL</label>
        <input class="input" type="url" formControlName="url" placeholder="https://..." required />
        <div class="error" *ngIf="form.controls.url.invalid && form.controls.url.touched">
          Informe uma URL válida
        </div>

        <label>Código (opcional)</label>
        <input class="input" type="text" formControlName="code" maxlength="5" pattern="^[A-Za-z0-9]{5}$" />

        <button class="btn" [disabled]="facade.loading() || form.invalid">Encurtar</button>
      </form>

      <div class="result" *ngIf="facade.result() as r">
        <p>Seu link: <strong>{{ r.code }}</strong></p>
        <p>Original: {{ r.originalUrl }}</p>
        <p>Criado em: {{ r.createdAt | date:'short' }}</p>
      </div>

      <div class="error" *ngIf="facade.error() as err">{{ err }}</div>
    </section>
  `,
  styles: [
    `.container{max-width:640px;margin:24px auto;padding:16px}`,
    `.input{width:100%;padding:8px;margin:4px 0}`,
    `.btn{padding:8px 12px;margin-top:8px}`,
    `.error{color:#c0392b;margin-top:8px}`,
    `.result{background:#f6f8fa;padding:12px;margin-top:12px;border-radius:6px}`
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export default class ShortenPageComponent {
  private readonly fb = inject(FormBuilder);
  readonly facade = inject(ShortenFacade);

  readonly form = this.fb.nonNullable.group({
    url: ['', [Validators.required, Validators.pattern(/https?:\/\/.+/)]],
    code: ['']
  });

  async onSubmit(): Promise<void> {
    if (this.form.invalid) return;
    const { url, code } = this.form.getRawValue();
    const payload: { url: string; code?: string } = { url };
    if (code && code.trim().length > 0) payload.code = code;
    await this.facade.create(payload as { url: string; code?: string });
  }
}