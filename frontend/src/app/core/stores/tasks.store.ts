import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { Task } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

interface TaskPayload {
  title: string;
  description: string;
  status: string;
  priority: string;
  dueDate: string | null;
  positionIndex: number;
  projectId: number | null;
  responsibleUserId: number | null;
  participantIds: number[];
}

@Injectable({ providedIn: 'root' })
export class TasksStore {
  private readonly http = inject(HttpClient);

  readonly items = signal<Task[]>([]);
  readonly mineItems = signal<Task[]>([]);
  readonly loading = signal(false);
  readonly mineLoading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly mineError = signal<string | null>(null);
  readonly grouped = computed(() => {
    const groups = new Map<string, Task[]>();

    for (const task of this.items()) {
      const bucket = groups.get(task.status) ?? [];
      bucket.push(task);
      bucket.sort((left, right) => left.positionIndex - right.positionIndex);
      groups.set(task.status, bucket);
    }

    return groups;
  });

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const tasks = await firstValueFrom(this.http.get<Task[]>('/api/tasks'));
      this.items.set(tasks);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar as tarefas.'));
    } finally {
      this.loading.set(false);
    }
  }

  async loadMine(): Promise<void> {
    this.mineLoading.set(true);
    this.mineError.set(null);

    try {
      const tasks = await firstValueFrom(this.http.get<Task[]>('/api/tasks/me'));
      this.mineItems.set(tasks);
    } catch (error) {
      this.mineError.set(getApiErrorMessage(error, 'Nao foi possivel carregar o calendario das suas tarefas.'));
    } finally {
      this.mineLoading.set(false);
    }
  }

  async create(payload: TaskPayload): Promise<void> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const created = await firstValueFrom(this.http.post<Task>('/api/tasks', payload));
      this.items.update((items) => [...items, created]);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel criar a tarefa.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  async update(taskId: number, payload: TaskPayload): Promise<void> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const updated = await firstValueFrom(this.http.put<Task>(`/api/tasks/${taskId}`, payload));
      this.items.update((items) => items.map((item) => (item.id === taskId ? updated : item)));
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel atualizar a tarefa.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  async updateStatus(taskId: number, status: string, positionIndex: number): Promise<void> {
    this.error.set(null);

    try {
      const updated = await firstValueFrom(
        this.http.patch<Task>(`/api/tasks/${taskId}/status`, {
          status,
          positionIndex
        })
      );

      this.items.update((items) => items.map((item) => (item.id === taskId ? updated : item)));
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel mover a tarefa.'));
      throw error;
    }
  }

  reset(): void {
    this.items.set([]);
    this.mineItems.set([]);
    this.loading.set(false);
    this.mineLoading.set(false);
    this.saving.set(false);
    this.error.set(null);
    this.mineError.set(null);
  }
}
