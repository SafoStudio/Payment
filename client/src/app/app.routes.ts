import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'forbidden', loadComponent: () => import('./features/auth/forbidden/forbidden.page').then(m => m.ForbiddenPage) },
  { path: '', redirectTo: '/forbidden', pathMatch: 'full' },
];
