import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth';

const CHANGE_PASSWORD_ROUTE = '/profile/change-password';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.currentUser();

  // Non authentifie : direction /login, en gardant la destination voulue.
  if (!user) {
    return router.createUrlTree(['/login'], {
      queryParams: { redirectTo: state.url },
    });
  }

  // Blocage strict des comptes en mot de passe temporaire (compte interne
  // cree par un admin) : SEULE /profile/change-password est accessible tant
  // que le flag est actif. Empeche la boucle infinie en laissant passer si
  // on est deja sur la page de changement.
  if (user.mustChangePassword && !state.url.startsWith(CHANGE_PASSWORD_ROUTE)) {
    return router.createUrlTree([CHANGE_PASSWORD_ROUTE]);
  }

  return true;
};