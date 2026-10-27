import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/auth.guard';
import { permissionGuard } from '@core/auth/permission.guard';

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
      {
        path: 'wallets',
        canActivate: [permissionGuard(['wallet:manage:own', 'wallet:read:all'])],
        children: [
          {
            path: '',
            loadComponent: () =>
              import('@features/wallets/wallets-page.component').then(
                (m) => m.WalletsPageComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import(
                '@features/wallets/wallet-detail/wallet-detail-page.component'
              ).then((m) => m.WalletDetailPageComponent),
          },
        ],
      },
      {
        path: 'payments',
        canActivate: [
          permissionGuard([
            'payment:read:own',
            'payment:read:all',
            'payment:manage:own',
          ]),
        ],
        children: [
          {
            path: '',
            loadComponent: () =>
              import('@features/payments/payments-page.component').then(
                (m) => m.PaymentsPageComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import(
                '@features/payments/payment-detail/payment-detail-page.component'
              ).then((m) => m.PaymentDetailPageComponent),
          },
        ],
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
