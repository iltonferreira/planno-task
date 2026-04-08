import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { KnowledgeBasePage } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

export interface KnowledgeBasePayload {
  title: string;
  summary: string | null;
  content: string;
  pinned: boolean;
}

@Injectable({ providedIn: 'root' })
export class KnowledgeBaseStore {
  private readonly http = inject(HttpClient);

  readonly items = signal<KnowledgeBasePage[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  async load(search?: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const suffix = search ? `?search=${encodeURIComponent(search)}` : '';
      const pages = await firstValueFrom(this.http.get<KnowledgeBasePage[]>(`/api/knowledge-base${suffix}`));
      this.items.set(pages);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar a base de conhecimento.'));
    } finally {
      this.loading.set(false);
    }
  }

  async create(payload: KnowledgeBasePayload): Promise<KnowledgeBasePage> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const created = await firstValueFrom(this.http.post<KnowledgeBasePage>('/api/knowledge-base', payload));
      this.items.update((items) => [created, ...items]);
      return created;
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel salvar a pagina.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  async update(id: number, payload: KnowledgeBasePayload): Promise<KnowledgeBasePage> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const updated = await firstValueFrom(this.http.put<KnowledgeBasePage>(`/api/knowledge-base/${id}`, payload));
      this.items.update((items) => items.map((item) => (item.id === id ? updated : item)));
      return updated;
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel atualizar a pagina.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  async remove(id: number): Promise<void> {
    this.error.set(null);

    try {
      await firstValueFrom(this.http.delete<void>(`/api/knowledge-base/${id}`));
      this.items.update((items) => items.filter((item) => item.id !== id));
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel remover a pagina.'));
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
