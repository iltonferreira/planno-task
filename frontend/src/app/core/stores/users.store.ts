import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { User } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

@Injectable({ providedIn: 'root' })
export class UsersStore {
  private readonly http = inject(HttpClient);

  readonly items = signal<User[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const users = await firstValueFrom(this.http.get<User[]>('/api/users'));
      this.items.set(users);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar os usuarios.'));
    } finally {
      this.loading.set(false);
    }
  }

  reset(): void {
    this.items.set([]);
    this.error.set(null);
    this.loading.set(false);
  }
}
