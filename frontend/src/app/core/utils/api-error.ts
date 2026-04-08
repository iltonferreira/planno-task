import { HttpErrorResponse } from '@angular/common/http';

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (error.error && typeof error.error.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }

    if (error.message) {
      return error.message;
    }
  }

  return fallback;
}
