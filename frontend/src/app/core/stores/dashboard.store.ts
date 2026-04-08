import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { DashboardSummary } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

@Injectable({ providedIn: 'root' })
export class DashboardStore {
  private readonly http = inject(HttpClient);

  readonly summary = signal<DashboardSummary | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const summary = await firstValueFrom(this.http.get<DashboardSummary>('/api/dashboard/summary'));
      this.summary.set(summary);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar o dashboard.'));
    } finally {
      this.loading.set(false);
    }
  }

  reset(): void {
    this.summary.set(null);
    this.loading.set(false);
    this.error.set(null);
  }
}
