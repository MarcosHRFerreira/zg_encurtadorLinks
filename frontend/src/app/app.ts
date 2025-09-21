import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="app-header">
      <nav class="nav">
        <a routerLink="/shorten" routerLinkActive="active">Encurtar</a>
        <a routerLink="/consultas" routerLinkActive="active">Consultas</a>
      </nav>
    </header>

    <router-outlet />
  `,
  styles: [
    `
      .app-header {
        padding: 12px 16px;
        display: flex;
        align-items: center;
        gap: 16px;
        border-bottom: 1px solid #e5e7eb;
      }
      .nav { display:flex; gap:12px; margin-left:auto }
      .nav a { color:#374151; text-decoration:none; padding:6px 8px; border-radius:6px }
      .nav a.active, .nav a:hover { background:#f3f4f6 }
    `,
  ],
})
export class App {}
