import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { GoogleCalendarConnectionStatus, GoogleCalendarEvent, Task } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

@Injectable({ providedIn: 'root' })
export class GoogleCalendarStore {
  private readonly http = inject(HttpClient);

  readonly status = signal<GoogleCalendarConnectionStatus | null>(null);
  readonly events = signal<GoogleCalendarEvent[]>([]);
  readonly loadingStatus = signal(false);
  readonly loadingEvents = signal(false);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly linkedTaskIds = computed(() => new Set(this.events()
    .filter((event) => event.linkedTaskId !== null)
    .map((event) => event.linkedTaskId as number)));

  async loadStatus(): Promise<void> {
    this.loadingStatus.set(true);
    this.error.set(null);

    try {
      const status = await firstValueFrom(
        this.http.get<GoogleCalendarConnectionStatus>('/api/integrations/google-calendar/status')
      );
      this.status.set(status);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel consultar a conexao do Google Calendar.'));
    } finally {
      this.loadingStatus.set(false);
    }
  }

  async loadEvents(start: string, end: string): Promise<void> {
    if (!this.status()?.connected) {
      this.events.set([]);
      return;
    }

    this.loadingEvents.set(true);
    this.error.set(null);

    try {
      const events = await firstValueFrom(
        this.http.get<GoogleCalendarEvent[]>('/api/integrations/google-calendar/events', {
          params: { start, end }
        })
      );
      this.events.set(events);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar os eventos do Google Calendar.'));
    } finally {
      this.loadingEvents.set(false);
    }
  }

  async createConnectUrl(): Promise<string> {
    this.busy.set(true);
    this.error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.post<{ authorizationUrl: string }>('/api/integrations/google-calendar/connect-url', {})
      );
      return response.authorizationUrl;
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel iniciar a conexao com o Google Calendar.'));
      throw error;
    } finally {
      this.busy.set(false);
    }
  }

  async disconnect(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);

    try {
      await firstValueFrom(this.http.delete<void>('/api/integrations/google-calendar/connection'));
      this.status.set(null);
      this.events.set([]);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel desconectar o Google Calendar.'));
      throw error;
    } finally {
      this.busy.set(false);
    }
  }

  async syncTask(taskId: number): Promise<void> {
    this.busy.set(true);
    this.error.set(null);

    try {
      await firstValueFrom(this.http.post(`/api/integrations/google-calendar/tasks/${taskId}/sync`, {}));
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel sincronizar a tarefa com o Google Calendar.'));
      throw error;
    } finally {
      this.busy.set(false);
    }
  }

  async importEvent(calendarId: string, eventId: string): Promise<Task> {
    this.busy.set(true);
    this.error.set(null);

    try {
      return await firstValueFrom(
        this.http.post<Task>('/api/integrations/google-calendar/import-event', null, {
          params: { calendarId, eventId }
        })
      );
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel importar o evento do Google Calendar.'));
      throw error;
    } finally {
      this.busy.set(false);
    }
  }

  reset(): void {
    this.status.set(null);
    this.events.set([]);
    this.loadingStatus.set(false);
    this.loadingEvents.set(false);
    this.busy.set(false);
    this.error.set(null);
  }
}
