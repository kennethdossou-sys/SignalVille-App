import {
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../auth/auth';

const PUBLIC_ENDPOINTS = ['/auth/register', '/auth/login', '/auth/refresh'];

function isPublicEndpoint(url: string): boolean {
  return PUBLIC_ENDPOINTS.some(endpoint => url.includes(endpoint));
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const accessToken = authService.getAccessToken();

  const authReq = accessToken
    ? req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      const isAuthError = error instanceof HttpErrorResponse && error.status === 401;
      const shouldAttemptRefresh = isAuthError && !isPublicEndpoint(req.url);

      if (!shouldAttemptRefresh) {
        return throwError(() => error);
      }

      return authService.refresh().pipe(
        switchMap(() => {
          const newToken = authService.getAccessToken();
          const retriedReq = req.clone({
            setHeaders: { Authorization: `Bearer ${newToken}` },
          });
          return next(retriedReq);
        }),
        catchError((refreshError: unknown) => {
          authService.forceLocalLogout();
          return throwError(() => refreshError);
        })
      );
    })
  );
};