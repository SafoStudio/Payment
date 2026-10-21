import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';
import { toObservable } from '@angular/core/rxjs-interop';
import { firstValueFrom, filter } from 'rxjs';

export function permissionGuard(requiredPermission: string): CanActivateFn {
  return async () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.loading()) {
      await firstValueFrom(
        toObservable(auth.loading).pipe(filter((loading) => !loading)),
      );
    }

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }

    if (!auth.hasPermission(requiredPermission)) {
      return router.createUrlTree(['/forbidden']);
    }

    return true;
  };
}
