import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@layout/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('@features/dashboard/dashboard-page.component').then(
            (m) => m.DashboardPageComponent,
          ),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  {
    path: 'auth',
    loadComponent: () =>
      import('@features/auth/auth-page/auth-page.component').then(
        (m) => m.AuthPageComponent,
      ),
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('@features/auth/forbidden/forbidden.page').then(
        (m) => m.ForbiddenPage,
      ),
  },
  {
    path: 'passkey-enrollment',
    loadComponent: () =>
      import('@features/auth/passkey-enrollment/passkey-enrollment.page').then(
        (m) => m.PasskeyEnrollmentPage,
      ),
  },
  { path: '**', redirectTo: '' },
];
