import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { TenantBillingMode } from '../../core/models/domain.models';
import { PlatformBillingAdminStore } from '../../core/stores/platform-billing-admin.store';
import { PlatformSubscriptionStore } from '../../core/stores/platform-subscription.store';

@Component({
  selector: 'app-workspace-admin-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './workspace-admin.page.html',
  styleUrl: './workspace-admin.page.scss'
})
export class WorkspaceAdminPageComponent {
  private readonly router = inject(Router);
  readonly platformSubscriptionStore = inject(PlatformSubscriptionStore);
  readonly adminStore = inject(PlatformBillingAdminStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly billingModes: TenantBillingMode[] = ['SUBSCRIPTION_REQUIRED', 'COMPLIMENTARY'];
  readonly customers = computed(() => this.adminStore.customers());
  readonly billingAdminAccess = computed(() => this.platformSubscriptionStore.isBillingAdminTenant());
  readonly complimentaryCount = computed(
    () => this.customers().filter((customer) => customer.billingMode === 'COMPLIMENTARY').length,
  );
  readonly paidCount = computed(
    () => this.customers().filter((customer) => customer.billingMode === 'SUBSCRIPTION_REQUIRED').length,
  );
  readonly activePaidCount = computed(
    () =>
      this.customers().filter(
        (customer) =>
          customer.billingMode === 'SUBSCRIPTION_REQUIRED' && customer.status === 'ACTIVE',
      ).length,
  );

  readonly form = this.formBuilder.nonNullable.group({
    tenantName: ['', Validators.required],
    tenantSlug: ['', Validators.required],
    tenantCnpj: [''],
    adminName: ['', Validators.required],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminCpf: ['', [Validators.required, Validators.minLength(11), Validators.maxLength(11)]],
    adminPassword: ['', [Validators.required, Validators.minLength(8)]],
    billingMode: ['SUBSCRIPTION_REQUIRED' as TenantBillingMode, Validators.required]
  });

  constructor() {
    effect(() => {
      const subscription = this.platformSubscriptionStore.subscription();
      if (!subscription) {
        return;
      }

      if (!this.platformSubscriptionStore.isBillingAdminTenant()) {
        this.adminStore.reset();
        void this.router.navigateByUrl('/payments');
        return;
      }

      if (!this.adminStore.overview() && !this.adminStore.loading()) {
        void this.adminStore.load();
      }
    });
  }

  fillSlugFromName(): void {
    const currentSlug = this.form.controls.tenantSlug.getRawValue().trim();
    if (currentSlug) {
      return;
    }

    const normalizedSlug = this.form.controls.tenantName
      .getRawValue()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');

    this.form.controls.tenantSlug.setValue(normalizedSlug);
  }

  billingModeLabel(mode: TenantBillingMode): string {
    return mode === 'COMPLIMENTARY' ? 'Cortesia' : 'Assinatura';
  }

  customerStatusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'Ativo';
      case 'PAST_DUE':
        return 'Pagamento pendente';
      case 'CANCELLED':
        return 'Cancelado';
      case 'COMPLIMENTARY':
        return 'Cortesia';
      case 'PENDING':
        return 'Aguardando confirmacao';
      default:
        return 'Nao iniciado';
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    await this.adminStore.provisionWorkspace({
      tenantName: rawValue.tenantName,
      tenantSlug: rawValue.tenantSlug,
      tenantCnpj: rawValue.tenantCnpj || null,
      adminName: rawValue.adminName,
      adminEmail: rawValue.adminEmail,
      adminCpf: rawValue.adminCpf,
      adminPassword: rawValue.adminPassword,
      billingMode: rawValue.billingMode
    });

    this.form.reset({
      tenantName: '',
      tenantSlug: '',
      tenantCnpj: '',
      adminName: '',
      adminEmail: '',
      adminCpf: '',
      adminPassword: '',
      billingMode: 'SUBSCRIPTION_REQUIRED'
    });
  }
}
