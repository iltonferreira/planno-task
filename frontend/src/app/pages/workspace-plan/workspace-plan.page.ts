import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';

import { AuthStore } from '../../core/stores/auth.store';
import { PlatformSubscriptionStore } from '../../core/stores/platform-subscription.store';

interface PlanStatusMeta {
  label: string;
  description: string;
}

@Component({
  selector: 'app-workspace-plan-page',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe],
  templateUrl: './workspace-plan.page.html',
  styleUrl: './workspace-plan.page.scss'
})
export class WorkspacePlanPageComponent {
  readonly authStore = inject(AuthStore);
  readonly platformSubscriptionStore = inject(PlatformSubscriptionStore);

  readonly subscription = computed(() => this.platformSubscriptionStore.subscription());
  readonly statusMeta = computed<PlanStatusMeta>(() => {
    if (this.subscription()?.billingMode === 'COMPLIMENTARY') {
      return {
        label: 'Cortesia',
        description: 'Esse workspace foi liberado manualmente pela equipe Planno e nao precisa pagar assinatura agora.'
      };
    }

    const status = this.subscription()?.status ?? 'NOT_STARTED';

    switch (status) {
      case 'ACTIVE':
        return {
          label: 'Ativo',
          description: 'O workspace esta liberado e a cobranca mensal segue pela sua conta Mercado Pago.'
        };
      case 'PAST_DUE':
        return {
          label: 'Pagamento pendente',
          description: 'Existe uma acao pendente na assinatura. Regularize para evitar bloqueio do workspace.'
        };
      case 'CANCELLED':
        return {
          label: 'Plano inativo',
          description: 'O acesso operacional foi bloqueado. Reative a assinatura para voltar a usar os modulos internos.'
        };
      case 'PENDING':
        return {
          label: 'Aguardando ativacao',
          description: 'O checkout ja foi iniciado, mas a assinatura ainda nao foi confirmada pelo Mercado Pago.'
        };
      default:
        return {
          label: 'Nao iniciado',
          description: 'Ative a assinatura do Planno para liberar o workspace da equipe.'
        };
    }
  });
  readonly actionLabel = computed(() => {
    if (this.subscription()?.billingMode === 'COMPLIMENTARY') {
      return 'Checkout nao necessario';
    }

    const status = this.subscription()?.status ?? 'NOT_STARTED';

    switch (status) {
      case 'ACTIVE':
        return 'Atualizar cobranca';
      case 'PAST_DUE':
        return 'Regularizar assinatura';
      case 'CANCELLED':
        return 'Reativar assinatura';
      default:
        return 'Ativar assinatura';
    }
  });

  constructor() {
    if (this.authStore.isAuthenticated() && !this.platformSubscriptionStore.subscription()) {
      void this.platformSubscriptionStore.load();
    }
  }

  async openCheckout(): Promise<void> {
    if (this.subscription()?.billingMode === 'COMPLIMENTARY') {
      return;
    }

    const subscription = await this.platformSubscriptionStore.createCheckout();
    if (subscription.checkoutUrl) {
      window.open(subscription.checkoutUrl, '_blank', 'noopener');
    }
  }
}
