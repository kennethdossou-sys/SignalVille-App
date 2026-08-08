import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { RoleName } from '../../shared/models/api.models';

/** Barriere de confort cote client : la vraie regle reste appliquee par le backend. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

export const roleGuard = (allowed: RoleName[]): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const user = auth.currentUser();

    if (!user) {
      return router.createUrlTree(['/login']);
    }
    return allowed.includes(user.role) ? true : router.createUrlTree(['/dashboard']);
  };
};
