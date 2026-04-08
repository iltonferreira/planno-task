import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ClientsStore } from '../../core/stores/clients.store';
import { PaymentsStore } from '../../core/stores/payments.store';
import { ProjectsStore } from '../../core/stores/projects.store';

@Component({
  selector: 'app-payments-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe],
  templateUrl: './payments.page.html',
  styleUrl: './payments.page.scss'
})
export class PaymentsPageComponent {
  readonly paymentsStore = inject(PaymentsStore);
  readonly clientsStore = inject(ClientsStore);
  readonly projectsStore = inject(ProjectsStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly statuses = ['PENDING', 'IN_PROCESS', 'APPROVED', 'REJECTED', 'CANCELLED', 'REFUNDED'];
  readonly highlightedPayments = computed(() => this.paymentsStore.items().slice(0, 8));
  readonly net = computed(() => this.paymentsStore.income() - this.paymentsStore.expenses());
  readonly pendingCount = computed(() =>
    this.paymentsStore.items().filter((payment) => ['PENDING', 'IN_PROCESS'].includes(payment.status))
      .length
  );

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
    amount: [0, Validators.min(0.01)],
    type: ['ONE_TIME', Validators.required],
    direction: ['INCOME', Validators.required],
    dueDate: [''],
    clientId: [0],
    projectId: [0]
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    await this.paymentsStore.create({
      title: rawValue.title,
      description: rawValue.description || null,
      amount: rawValue.amount,
      type: rawValue.type,
      direction: rawValue.direction,
      provider: 'MANUAL',
      dueDate: rawValue.dueDate || null,
      clientId: rawValue.clientId || null,
      projectId: rawValue.projectId || null,
      subscriptionId: null
    });

    this.form.reset({
      title: '',
      description: '',
      amount: 0,
      type: 'ONE_TIME',
      direction: 'INCOME',
      dueDate: '',
      clientId: 0,
      projectId: 0
    });
  }

  async updateStatus(paymentId: number, status: string): Promise<void> {
    await this.paymentsStore.updateStatus(paymentId, status);
  }
}
