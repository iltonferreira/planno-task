import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';

import { AuthResponse, User } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

interface LoginPayload {
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenStorageKey = 'planno.token';
  private readonly userStorageKey = 'planno.user';

  readonly token = signal<string | null>(globalThis.localStorage?.getItem(this.tokenStorageKey) ?? null);
  readonly user = signal<User | null>(this.restoreUser());
  readonly modalOpen = signal(this.token() === null);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly isAuthenticated = computed(() => Boolean(this.token()));

  open(): void {
    this.error.set(null);
    this.modalOpen.set(true);
  }

  ensureModalOpen(): void {
    if (!this.isAuthenticated()) {
      this.open();
    }
  }

  close(): void {
    this.modalOpen.set(false);
    this.error.set(null);
  }

  async login(payload: LoginPayload): Promise<void> {
    this.submitting.set(true);
    this.error.set(null);

    try {
      const response = await firstValueFrom(this.http.post<AuthResponse>('/api/auth/login', payload));
      this.setSession(response);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel realizar o login.'));
    } finally {
      this.submitting.set(false);
    }
  }

  async refreshMe(): Promise<void> {
    if (!this.token()) {
      return;
    }

    try {
      const user = await firstValueFrom(this.http.get<User>('/api/users/me'));
      this.user.set(user);
      globalThis.localStorage?.setItem(this.userStorageKey, JSON.stringify(user));
    } catch {
      this.clearSession(false);
      this.open();
    }
  }

  logout(): void {
    this.clearSession(true);
  }

  clearSession(openModal: boolean): void {
    this.token.set(null);
    this.user.set(null);
    globalThis.localStorage?.removeItem(this.tokenStorageKey);
    globalThis.localStorage?.removeItem(this.userStorageKey);
    if (openModal) {
      this.open();
      void this.router.navigateByUrl('/dashboard');
    }
  }

  private setSession(response: AuthResponse): void {
    this.token.set(response.token);
    this.user.set(response.user);
    globalThis.localStorage?.setItem(this.tokenStorageKey, response.token);
    globalThis.localStorage?.setItem(this.userStorageKey, JSON.stringify(response.user));
    this.close();
    void this.router.navigateByUrl('/dashboard');
  }

  private restoreUser(): User | null {
    const rawUser = globalThis.localStorage?.getItem(this.userStorageKey);
    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as User;
    } catch {
      globalThis.localStorage?.removeItem(this.userStorageKey);
      return null;
    }
  }
}
