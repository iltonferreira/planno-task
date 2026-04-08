import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { resolveApiUrl } from '../config/runtime-config';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const normalizedRequest = req.url.startsWith('/api')
    ? req.clone({ url: resolveApiUrl(req.url) })
    : req;
  const token = globalThis.localStorage?.getItem('planno.token');
  const isPublicRequest = !token || normalizedRequest.url.includes('/api/auth/login');

  const authorizedRequest = isPublicRequest
    ? normalizedRequest
    : normalizedRequest.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 402 &&
        !authorizedRequest.url.includes('/api/platform-billing')
      ) {
        void router.navigateByUrl('/workspace-plan');
      }

      return throwError(() => error);
    }),
  );
};
