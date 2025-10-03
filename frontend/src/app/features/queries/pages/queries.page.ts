import { Component, ChangeDetectionStrategy, inject, OnInit } from '@angular/core';
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
            <p><strong>URL curta:</strong> <a [href]="backendOrigin + '/' + s.code" target="_blank" rel="noopener">{{ backendOrigin + '/' + s.code }}</a></p>
            <div class="summary">
              <div class="kpis">
                <div class="kpi"><span class="label">Total (últimos 7 dias)</span><span class="value">{{ s.last7DaysHits }}</span></div>
              </div>
              <table class="table">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th class="numeric">Acessos</th>
                  </tr>
                </thead>
                <ng-container *ngIf="dailyWithHits(s).length > 0; else emptyDaily">
                  <tbody>
                    <tr *ngFor="let d of dailyWithHitsSorted(s)">
                      <td>{{ d.date | date:'dd/MM/yyyy' }}</td>
                      <td class="numeric">{{ d.hits }}</td>
                    </tr>
                  </tbody>
                </ng-container>
              </table>
              <ng-template #emptyDaily>
                <p class="muted">Sem acessos nos últimos 7 dias</p>
              </ng-template>
            </div>
          </div>
        </article>

        <article class="card">
          <div class="row">
            <h3>Ranking de códigos mais acessados</h3>
            <button class="btn ghost" (click)="reloadRanking()" [disabled]="facade.rankingLoading()">Recarregar</button>
          </div>

          <div class="loading" *ngIf="facade.rankingLoading()" aria-live="polite">Recarregando...</div>

          <ng-container *ngIf="!facade.rankingLoading()">
            <div class="error" *ngIf="facade.rankingError() as err">{{ err }}</div>

            <ng-container *ngIf="!facade.rankingError()">
              <ng-container *ngIf="facade.ranking() as items">
                <ng-container *ngIf="items.length > 0; else emptyRanking">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Código</th>
                        <th>URL curta</th>
                        <th class="numeric">Acessos</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr *ngFor="let item of items; let i = index; trackBy: trackByCode">
                        <td>{{ i + 1 }}</td>
                        <td class="code">{{ item.code }}</td>
                        <td class="truncate"><a [href]="backendOrigin + '/' + item.code" target="_blank" rel="noopener">{{ backendOrigin + '/' + item.code }}</a></td>
                        <td class="numeric">{{ item.hits }}</td>
                      </tr>
                    </tbody>
                  </table>
                </ng-container>
              </ng-container>
              <ng-template #emptyRanking>
                <p class="muted">Sem itens no ranking ainda</p>
              </ng-template>
            </ng-container>
          </ng-container>
        </article>

        <article class="card">
          <div class="row">
            <h3>Estatísticas gerais</h3>
            <div class="controls">
              <label>
                Tamanho da página
                <select [value]="facade.statsPageSize()" (change)="onPageSizeChange($event)">
                  <option *ngFor="let opt of pageSizeOptions" [value]="opt">{{ opt }}</option>
                </select>
              </label>
              <div class="pager">
                <button class="btn ghost" (click)="prevPage()" [disabled]="facade.statsPageLoading() || facade.statsPage()?.first">Anterior</button>
                <button class="btn ghost" (click)="nextPage()" [disabled]="facade.statsPageLoading() || facade.statsPage()?.last">Próxima</button>
              </div>
            </div>
          </div>

          <div class="loading" *ngIf="facade.statsPageLoading()" aria-live="polite">Carregando...</div>
          <div class="error" *ngIf="facade.statsPageError() as err">{{ err }}</div>

          <ng-container *ngIf="!facade.statsPageLoading() && !facade.statsPageError()">
            <ng-container *ngIf="facade.statsPage() as page">
              <ng-container *ngIf="!page.empty; else emptyStats">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Código</th>
                      <th>Original</th>
                      <th class="numeric">Acessos</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let s of page.content; trackBy: trackByCode">
                      <td>{{ s.code }}</td>
                      <td class="truncate"><a [href]="s.originalUrl" target="_blank" rel="noopener">{{ s.originalUrl }}</a></td>
                      <td class="numeric">{{ s.hits }}</td>
                    </tr>
                  </tbody>
                </table>
                <p class="muted">Página {{ page.number + 1 }} de {{ page.totalPages }} — {{ page.totalElements }} registro(s)</p>
              </ng-container>
            </ng-container>
            <ng-template #emptyStats>
              <p class="muted">Nenhuma estatística disponível</p>
            </ng-template>
          </ng-container>
        </article>
      </div>
    </section>
  `,
  styles: [
    `.container{max-width:960px;margin:24px auto;padding:16px}`,
    `.cards{display:grid;grid-template-columns:1fr;gap:16px}@media(min-width:720px){.cards{grid-template-columns:1fr 1fr}}`,
    `.card{background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:16px}`,
    `.row{display:flex;align-items:center;justify-content:space-between}`,
    `.controls{display:flex;gap:12px;align-items:center}`,
    `.pager{display:flex;gap:8px}`,
    `.input{width:100%;padding:8px;margin:4px 0}`,
    `.btn{padding:8px 12px;margin-top:8px;cursor:pointer}`,
    `.btn.ghost{background:transparent;border:1px solid #e5e7eb}`,
    `.error{color:#c0392b;margin-top:8px}`,
    `.loading{color:#6b7280;margin-top:8px}`,
    `.muted{color:#6b7280;margin-top:8px}`,
    `.result{background:#f6f8fa;padding:12px;margin-top:12px;border-radius:6px}`,
    `.ranking{margin:8px 0;padding-left:20px}`,
    `.ranking li{display:flex;gap:8px;align-items:center;padding:4px 0}`,
    `.code{font-weight:600}`,
    `.hits{color:#6b7280}`,
    `.table{width:100%;border-collapse:collapse;margin-top:8px}`,
    `.table th,.table td{border-bottom:1px solid #e5e7eb;padding:8px;text-align:left}`,
    `.table th.numeric,.table td.numeric{text-align:right}`,
    `.truncate{max-width:380px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}`,
    `.summary{margin-top:8px}`,
    `.kpis{display:flex;gap:16px;margin:8px 0}`,
    `.kpi{background:#f6f8fa;border:1px solid #e5e7eb;border-radius:6px;padding:8px 12px}`,
    `.kpi .label{display:block;color:#6b7280;font-size:12px}`,
    `.kpi .value{display:block;font-size:18px;font-weight:600}`
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export default class QueriesPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly facade = inject(QueriesFacade);
  readonly backendOrigin: string = (() => {
    const hasWindow = typeof window !== 'undefined';
    const raw = hasWindow && window.__ENV__?.API_BASE_URL ? String(window.__ENV__?.API_BASE_URL) : '';
    if (raw) {
      // Remove barra final e sufixo /api se presente
      return raw.replace(/\/$/, '').replace(/\/api$/, '');
    }
    // Fallback para backend padrão em dev: porta 8081
    if (hasWindow) return `${window.location.protocol}//${window.location.hostname}:8081`;
    return 'http://localhost:8081';
  })();

  readonly statsForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9]{5}$/)]]
  });

  readonly pageSizeOptions = [10, 20, 30, 40, 50] as const;

  ngOnInit(): void {
    void this.facade.fetchRanking();
    void this.facade.fetchStatsPage();
  }

  async onStatsSubmit(): Promise<void> {
    if (this.statsForm.invalid) return;
    const { code } = this.statsForm.getRawValue();
    await this.facade.fetchStats(code);
  }

  async reloadRanking(): Promise<void> {
    await this.facade.fetchRanking();
  }

  async nextPage(): Promise<void> {
    await this.facade.nextStatsPage();
  }

  async prevPage(): Promise<void> {
    await this.facade.prevStatsPage();
  }

  async onPageSizeChange(event: Event): Promise<void> {
    const target = event.target as HTMLSelectElement;
    const size = parseInt(target.value, 10);
    await this.facade.setStatsPageSize(size);
  }

  trackByCode = (_: number, item: { code: string }): string => item.code;

  
  dailyWithHits(s: { daily?: ReadonlyArray<{ date: string | Date; hits: number | string }> }): ReadonlyArray<{ date: string | Date; hits: number | string }>{
    const list = s?.daily ?? [];
    return list.filter(d => Number(d.hits) > 0);
  }

  dailyWithHitsSorted(s: { daily?: ReadonlyArray<{ date: string | Date; hits: number | string }> }): ReadonlyArray<{ date: string | Date; hits: number | string }>{
    const filtered = this.dailyWithHits(s);
    return filtered.slice().sort((a, b) => new Date(b.date as any).getTime() - new Date(a.date as any).getTime());
  }
}