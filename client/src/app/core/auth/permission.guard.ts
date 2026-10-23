import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { firstValueFrom, filter } from 'rxjs';
import { AuthService } from './auth.service';

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
      return router.createUrlTree(['/auth']);
    }

    if (!auth.hasPermission(requiredPermission)) {
      return router.createUrlTree(['/forbidden']);
    }

    return true;
  };
}
