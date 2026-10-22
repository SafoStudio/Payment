import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'auth',
    loadComponent: () =>
      import('./features/auth/auth-page/auth-page.component').then((m) => m.AuthPageComponent),
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./features/auth/forbidden/forbidden.page').then((m) => m.ForbiddenPage),
  },
  {
    path: 'passkey-enrollment',
    loadComponent: () =>
      import('./features/auth/passkey-enrollment/passkey-enrollment.page').then((m) => m.PasskeyEnrollmentPage),
  },
  {
    path: '',
    redirectTo: '/auth',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: '/auth',
  },
];
