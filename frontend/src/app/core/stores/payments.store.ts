import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { Payment } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

export interface PaymentPayload {
  title: string;
  description: string | null;
  amount: number;
  type: string;
  direction: string;
  provider: string;
  dueDate: string | null;
  clientId: number | null;
  projectId: number | null;
  subscriptionId: number | null;
}

@Injectable({ providedIn: 'root' })
export class PaymentsStore {
  private readonly http = inject(HttpClient);

  readonly items = signal<Payment[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly income = computed(() =>
    this.items()
      .filter((payment) => payment.direction === 'INCOME' && payment.status === 'APPROVED')
      .reduce((total, payment) => total + payment.amount, 0)
  );

  readonly expenses = computed(() =>
    this.items()
      .filter((payment) => payment.direction === 'EXPENSE' && payment.status === 'APPROVED')
      .reduce((total, payment) => total + payment.amount, 0)
  );

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const payments = await firstValueFrom(this.http.get<Payment[]>('/api/payments'));
      this.items.set(payments);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar os pagamentos.'));
    } finally {
      this.loading.set(false);
    }
  }

  async create(payload: PaymentPayload): Promise<Payment> {
    this.saving.set(true);
    this.error.set(null);

    try {
      const created = await firstValueFrom(this.http.post<Payment>('/api/payments', payload));
      this.items.update((items) => [created, ...items]);
      return created;
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel salvar o pagamento.'));
      throw error;
    } finally {
      this.saving.set(false);
    }
  }

  async updateStatus(paymentId: number, status: string): Promise<void> {
    this.error.set(null);

    try {
      const updated = await firstValueFrom(
        this.http.patch<Payment>(`/api/payments/${paymentId}/status`, { status })
      );

      this.items.update((items) => items.map((item) => (item.id === paymentId ? updated : item)));
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel atualizar o pagamento.'));
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
