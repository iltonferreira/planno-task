import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  PlatformBillingAdminOverview,
  PlatformPaymentLinkResult,
  PlatformWorkspaceProvisionResult,
  TenantBillingMode,
} from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

export interface PlatformPaymentLinkPayload {
  tenantId: number | null;
  title: string;
  description: string | null;
  payerEmail: string | null;
  amount: number;
}

export interface PlatformWorkspaceProvisionPayload {
  tenantName: string;
  tenantSlug: string;
  tenantCnpj: string | null;
  adminName: string;
  adminEmail: string;
  adminCpf: string;
  adminPassword: string;
  billingMode: TenantBillingMode;
}

@Injectable({ providedIn: 'root' })
export class PlatformBillingAdminStore {
  private readonly http = inject(HttpClient);

  readonly overview = signal<PlatformBillingAdminOverview | null>(null);
  readonly lastPaymentLink = signal<PlatformPaymentLinkResult | null>(null);
  readonly lastProvisionedWorkspace = signal<PlatformWorkspaceProvisionResult | null>(null);
  readonly loading = signal(false);
  readonly generatingLink = signal(false);
  readonly provisioning = signal(false);
  readonly error = signal<string | null>(null);

  readonly customers = computed(() => this.overview()?.customers ?? []);
  readonly mercadoPagoSubscriptions = computed(() => this.overview()?.mercadoPagoSubscriptions ?? []);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const overview = await firstValueFrom(
        this.http.get<PlatformBillingAdminOverview>('/api/platform-billing/admin/overview'),
      );
      this.overview.set(overview);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar o painel de receitas.'));
      throw error;
    } finally {
      this.loading.set(false);
    }
  }

  async createPaymentLink(payload: PlatformPaymentLinkPayload): Promise<PlatformPaymentLinkResult> {
    this.generatingLink.set(true);
    this.error.set(null);

    try {
      const result = await firstValueFrom(
        this.http.post<PlatformPaymentLinkResult>('/api/platform-billing/admin/payment-links', payload),
      );
      this.lastPaymentLink.set(result);
      return result;
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel gerar o link avulso.'));
      throw error;
    } finally {
      this.generatingLink.set(false);
    }
  }

  async provisionWorkspace(
    payload: PlatformWorkspaceProvisionPayload,
  ): Promise<PlatformWorkspaceProvisionResult> {
    this.provisioning.set(true);
    this.error.set(null);

    try {
      const result = await firstValueFrom(
        this.http.post<PlatformWorkspaceProvisionResult>(
          '/api/platform-billing/admin/customers',
          payload,
        ),
      );
      this.lastProvisionedWorkspace.set(result);
      await this.load();
      return result;
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel provisionar o workspace.'));
      throw error;
    } finally {
      this.provisioning.set(false);
    }
  }

  reset(): void {
    this.overview.set(null);
    this.lastPaymentLink.set(null);
    this.lastProvisionedWorkspace.set(null);
    this.loading.set(false);
    this.generatingLink.set(false);
    this.provisioning.set(false);
    this.error.set(null);
  }
}
