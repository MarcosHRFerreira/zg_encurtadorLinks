import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { QueriesFacade } from '../state/queries.facade';

@Component({
  selector: 'app-queries-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="container">
      <h2>Consultas</h2>

      <div class="cards">
        <article class="card">
          <h3>Estatísticas por código</h3>
          <form [formGroup]="statsForm" (ngSubmit)="onStatsSubmit()">
            <label for="code">Código</label>
            <input id="code" class="input" type="text" formControlName="code" maxlength="5" pattern="^[A-Za-z0-9]{5}$" placeholder="ABCDE" required />
            <button class="btn" [disabled]="facade.statsLoading() || statsForm.invalid">Buscar</button>
          </form>

          <div class="error" *ngIf="facade.statsError() as err">{{ err }}</div>

          <div class="result" *ngIf="facade.stats() as s">
            <p><strong>Código:</strong> {{ s.code }}</p>
            <p><strong>Original:</strong> <a [href]="s.originalUrl" target="_blank" rel="noopener">{{ s.originalUrl }}</a></p>
            <p><strong>Acessos:</strong> {{ s.hits }}</p>
          </div>
        </article>

        <article class="card">
          <div class="row">
            <h3>Ranking</h3>
            <button class="btn ghost" (click)="reloadRanking()" [disabled]="facade.rankingLoading()">Recarregar</button>
          </div>

          <div class="error" *ngIf="facade.rankingError() as err">{{ err }}</div>
          <ol class="ranking" *ngIf="facade.ranking() as items">
            <li *ngFor="let item of items; trackBy: trackByCode">
              <span class="code">{{ item.code }}</span>
              <span class="hits">{{ item.hits }} acesso(s)</span>
            </li>
          </ol>
        </article>
      </div>
    </section>
  `,
  styles: [
    `.container{max-width:960px;margin:24px auto;padding:16px}`,
    `.cards{display:grid;grid-template-columns:1fr;gap:16px}@media(min-width:720px){.cards{grid-template-columns:1fr 1fr}}`,
    `.card{background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:16px}`,
    `.row{display:flex;align-items:center;justify-content:space-between}`,
    `.input{width:100%;padding:8px;margin:4px 0}`,
    `.btn{padding:8px 12px;margin-top:8px;cursor:pointer}`,
    `.btn.ghost{background:transparent;border:1px solid #e5e7eb}`,
    `.error{color:#c0392b;margin-top:8px}`,
    `.result{background:#f6f8fa;padding:12px;margin-top:12px;border-radius:6px}`,
    `.ranking{margin:8px 0;padding-left:20px}`,
    `.ranking li{display:flex;gap:8px;align-items:center;padding:4px 0}`,
    `.code{font-weight:600}`,
    `.hits{color:#6b7280}`
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export default class QueriesPageComponent {
  private readonly fb = inject(FormBuilder);
  readonly facade = inject(QueriesFacade);

  readonly statsForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9]{5}$/)]]
  });

  ngOnInit(): void {
    void this.facade.fetchRanking();
  }

  async onStatsSubmit(): Promise<void> {
    if (this.statsForm.invalid) return;
    const { code } = this.statsForm.getRawValue();
    await this.facade.fetchStats(code);
  }

  async reloadRanking(): Promise<void> {
    await this.facade.fetchRanking();
  }

  trackByCode = (_: number, item: { code: string }): string => item.code;
}