import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { PlatformSubscription } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

@Injectable({ providedIn: 'root' })
export class PlatformSubscriptionStore {
  private readonly http = inject(HttpClient);

  readonly subscription = signal<PlatformSubscription | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly requiresAction = computed(() => this.subscription()?.requiresAction ?? false);
  readonly isBillingAdminTenant = computed(() => this.subscription()?.billingAdminTenant ?? false);
  readonly isComplimentaryTenant = computed(
    () => this.subscription()?.billingMode === 'COMPLIMENTARY',
  );
  readonly isAccessRestricted = computed(
    () =>
      !this.isBillingAdminTenant() &&
      !this.isComplimentaryTenant() &&
      this.subscription()?.status !== 'ACTIVE',
  );

  async load(): Promise<PlatformSubscription | null> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const subscription = await firstValueFrom(
        this.http.get<PlatformSubscription>('/api/platform-billing/me'),
      );
      this.subscription.set(subscription);
      return subscription;
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar o plano do workspace.'));
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  async createCheckout(): Promise<PlatformSubscription> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const subscription = await firstValueFrom(
        this.http.post<PlatformSubscription>('/api/platform-billing/checkout', {}),
      );
      this.subscription.set(subscription);
      return subscription;
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel iniciar a assinatura do workspace.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  reset(): void {
    this.subscription.set(null);
    this.loading.set(false);
    this.saving.set(false);
    this.error.set(null);
  }
}
