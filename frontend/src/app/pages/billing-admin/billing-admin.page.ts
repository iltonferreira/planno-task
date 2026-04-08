import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { PlatformBillingAdminStore } from '../../core/stores/platform-billing-admin.store';
import { PlatformSubscriptionStore } from '../../core/stores/platform-subscription.store';

@Component({
  selector: 'app-billing-admin-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './billing-admin.page.html',
  styleUrl: './billing-admin.page.scss',
})
export class BillingAdminPageComponent {
  private readonly router = inject(Router);
  readonly platformSubscriptionStore = inject(PlatformSubscriptionStore);
  readonly adminStore = inject(PlatformBillingAdminStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly form = this.formBuilder.nonNullable.group({
    tenantId: [0],
    title: ['', Validators.required],
    description: [''],
    payerEmail: [''],
    amount: [39.9, Validators.min(0.01)],
  });
  readonly overview = computed(() => this.adminStore.overview());
  readonly customers = computed(() => this.adminStore.customers());
  readonly mercadoPagoSubscriptions = computed(() => this.adminStore.mercadoPagoSubscriptions());
  readonly billingAdminAccess = computed(() => this.platformSubscriptionStore.isBillingAdminTenant());
  readonly projectedRevenue = computed(() => this.overview()?.projectedMonthlyRevenue ?? 0);
  readonly pendingRevenue = computed(() => this.overview()?.pendingMonthlyRevenue ?? 0);
  readonly mercadoPagoRevenue = computed(() => this.overview()?.mercadoPagoProjectedRevenue ?? 0);

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

  syncTenantContext(): void {
    const tenantId = this.form.controls.tenantId.getRawValue();
    const customer = this.customers().find((item) => item.tenantId === tenantId);
    if (!customer) {
      return;
    }

    this.form.patchValue({
      payerEmail: customer.payerEmail ?? this.form.controls.payerEmail.getRawValue(),
      title: this.form.controls.title.getRawValue() || `Cobranca avulsa - ${customer.tenantName}`,
    });
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    const link = await this.adminStore.createPaymentLink({
      tenantId: rawValue.tenantId || null,
      title: rawValue.title,
      description: rawValue.description || null,
      payerEmail: rawValue.payerEmail || null,
      amount: rawValue.amount,
    });

    this.form.reset({
      tenantId: 0,
      title: '',
      description: '',
      payerEmail: '',
      amount: 39.9,
    });

    if (link.checkoutUrl) {
      window.open(link.checkoutUrl, '_blank', 'noopener');
    }
  }
}
