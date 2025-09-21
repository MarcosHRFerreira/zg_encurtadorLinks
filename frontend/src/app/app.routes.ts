import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'shorten' },
  {
    path: 'shorten',
    loadComponent: () => import('./features/shorten/pages/shorten.page').then(m => m.default),
  },
  {
    path: 'consultas',
    loadComponent: () => import('./features/queries/pages/queries.page').then(m => m.default),
  },
];
