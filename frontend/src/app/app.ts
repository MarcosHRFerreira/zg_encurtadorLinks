import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="app-header">
      <a
        class="brand"
        href="https://zgsolucoes.com.br/"
        target="_blank"
        rel="noopener"
        aria-label="ZG Soluções - abrir site oficial"
      >
        ZG Soluções
      </a>

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
      .brand {
        font-family: 'Poppins', system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', 'Liberation Sans', 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';
        font-weight: 600;
        font-size: 20px;
        letter-spacing: 0.2px;
        color: #111827;
        text-decoration: none;
      }
      .nav { display:flex; gap:12px; margin-left:auto }
      .nav a { color:#374151; text-decoration:none; padding:6px 8px; border-radius:6px }
      .nav a.active, .nav a:hover { background:#f3f4f6 }
    `,
  ],
})
export class App {}
