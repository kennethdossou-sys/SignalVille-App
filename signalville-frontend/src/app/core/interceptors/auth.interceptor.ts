import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

/** Routes publiques : elles ne portent pas de Bearer. */
const PUBLIC_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh'];

/**
 * Routes ou un 401 ne doit jamais declencher de refresh : les trois routes
 * publiques ci-dessus, plus /auth/logout qui est authentifie mais dont l'echec
 * signifie que la session est deja morte.
 */
const NO_RETRY_ENDPOINTS = [...PUBLIC_ENDPOINTS, '/auth/logout'];

/**
 * Pose le header Bearer sur les appels a l'API, puis, sur 401, tente un
 * rafraichissement unique avant de rejouer la requete d'origine.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const isApiCall = request.url.startsWith(environment.apiUrl);
  const isPublicCall = PUBLIC_ENDPOINTS.some((endpoint) => request.url.includes(endpoint));
  const isRetryable = !NO_RETRY_ENDPOINTS.some((endpoint) => request.url.includes(endpoint));

  const authorized =
    isApiCall && !isPublicCall && auth.accessToken
      ? request.clone({ setHeaders: { Authorization: `Bearer ${auth.accessToken}` } })
      : request;

  return next(authorized).pipe(
    catchError((error: HttpErrorResponse) => {
      const canRetry = error.status === 401 && isApiCall && isRetryable && auth.refreshToken;
      if (!canRetry) {
        return throwError(() => error);
      }

      return auth.refresh().pipe(
        switchMap((response) =>
          next(request.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } })),
        ),
        catchError((refreshError) => {
          // Refresh expire ou revoque : la session est morte, on repart du login.
          auth.clear();
          void router.navigate(['/login']);
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
