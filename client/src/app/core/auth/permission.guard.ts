import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { firstValueFrom, filter } from 'rxjs';
import { AuthService } from './auth.service';

export function permissionGuard(
  required: string | readonly string[],
): CanActivateFn {
  const requiredList: readonly string[] =
    typeof required === 'string' ? [required] : required;

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

    if (!auth.hasAnyPermission([...requiredList])) {
      return router.createUrlTree(['/forbidden']);
    }

    return true;
  };
}
