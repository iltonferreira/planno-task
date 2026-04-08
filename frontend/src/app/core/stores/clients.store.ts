import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { Client } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

interface ClientPayload {
  name: string;
  email: string;
  phone: string;
  document: string;
}

@Injectable({ providedIn: 'root' })
export class ClientsStore {
  private readonly http = inject(HttpClient);

  readonly items = signal<Client[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const clients = await firstValueFrom(this.http.get<Client[]>('/api/clients'));
      this.items.set(clients);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar os clientes.'));
    } finally {
      this.loading.set(false);
    }
  }

  async create(payload: ClientPayload): Promise<void> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const created = await firstValueFrom(this.http.post<Client>('/api/clients', payload));
      this.items.update((items) => [created, ...items]);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel salvar o cliente.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  async remove(id: number): Promise<void> {
    this.error.set(null);

    try {
      await firstValueFrom(this.http.delete<void>(`/api/clients/${id}`));
      this.items.update((items) => items.filter((client) => client.id !== id));
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel remover o cliente.'));
      throw error;
    }
  }

  reset(): void {
    this.items.set([]);
    this.loading.set(false);
    this.saving.set(false);
    this.error.set(null);
  }
}
