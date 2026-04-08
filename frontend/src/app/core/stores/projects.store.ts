import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { Project } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

interface ProjectPayload {
  name: string;
  description: string;
  status: string;
  budget: number | null;
  startDate: string | null;
  endDate: string | null;
  clientId: number | null;
  ownerUserId: number | null;
}

@Injectable({ providedIn: 'root' })
export class ProjectsStore {
  private readonly http = inject(HttpClient);

  readonly items = signal<Project[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const projects = await firstValueFrom(this.http.get<Project[]>('/api/projects'));
      this.items.set(projects);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar os projetos.'));
    } finally {
      this.loading.set(false);
    }
  }

  async create(payload: ProjectPayload): Promise<void> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const created = await firstValueFrom(this.http.post<Project>('/api/projects', payload));
      this.items.update((items) => [created, ...items]);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel criar o projeto.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  async update(projectId: number, payload: ProjectPayload): Promise<void> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const updated = await firstValueFrom(this.http.put<Project>(`/api/projects/${projectId}`, payload));
      this.items.update((items) => items.map((item) => (item.id === projectId ? updated : item)));
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel atualizar o projeto.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  reset(): void {
    this.items.set([]);
    this.loading.set(false);
    this.saving.set(false);
    this.error.set(null);
  }
}
