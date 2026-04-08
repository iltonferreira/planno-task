import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = globalThis.localStorage?.getItem('planno.token');
  const isPublicRequest = !token || req.url.includes('/api/auth/login');

  const authorizedRequest = isPublicRequest
    ? req
    : req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 402 &&
        !req.url.includes('/api/platform-billing')
      ) {
        void router.navigateByUrl('/workspace-plan');
      }

      return throwError(() => error);
    }),
  );
};
