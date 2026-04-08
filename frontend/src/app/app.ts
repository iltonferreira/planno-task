import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

import { AuthStore } from './core/stores/auth.store';
import { ClientsStore } from './core/stores/clients.store';
import { DashboardStore } from './core/stores/dashboard.store';
import { DocumentsStore } from './core/stores/documents.store';
import { GoogleCalendarStore } from './core/stores/google-calendar.store';
import { KnowledgeBaseStore } from './core/stores/knowledge-base.store';
import { PaymentsStore } from './core/stores/payments.store';
import { PlatformBillingAdminStore } from './core/stores/platform-billing-admin.store';
import { PlatformSubscriptionStore } from './core/stores/platform-subscription.store';
import { ProjectsStore } from './core/stores/projects.store';
import { TasksStore } from './core/stores/tasks.store';
import { ThemeStore } from './core/stores/theme.store';
import { UsersStore } from './core/stores/users.store';
import { AuthModalComponent } from './shared/components/auth-modal/auth-modal.component';

interface NavigationItem {
  path: string;
  label: string;
  caption: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, AuthModalComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly authStore = inject(AuthStore);
  private readonly themeStore = inject(ThemeStore);
  private readonly router = inject(Router);
  private readonly dashboardStore = inject(DashboardStore);
  private readonly usersStore = inject(UsersStore);
  private readonly clientsStore = inject(ClientsStore);
  private readonly projectsStore = inject(ProjectsStore);
  private readonly tasksStore = inject(TasksStore);
  private readonly paymentsStore = inject(PaymentsStore);
  private readonly platformBillingAdminStore = inject(PlatformBillingAdminStore);
  private readonly documentsStore = inject(DocumentsStore);
  private readonly googleCalendarStore = inject(GoogleCalendarStore);
  private readonly knowledgeBaseStore = inject(KnowledgeBaseStore);
  readonly platformSubscriptionStore = inject(PlatformSubscriptionStore);
  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  readonly sidebarOpen = signal(false);
  private readonly baseNavigation: NavigationItem[] = [
    { path: '/dashboard', label: 'Dashboard', caption: 'Visao geral da operacao' },
    { path: '/workspace-plan', label: 'Meu plano', caption: 'Assinatura da plataforma' },
    { path: '/clients', label: 'Clientes', caption: 'Relacionamentos e CRM' },
    { path: '/projects', label: 'Projetos', caption: 'Roadmap de entrega' },
    { path: '/tasks', label: 'Tarefas', caption: 'Kanban do dia a dia' },
    { path: '/calendar', label: 'Calendario', caption: 'Agenda pessoal de tarefas' },
    {
      path: '/workspace-admin',
      label: 'Provisionamento',
      caption: 'Criar workspaces pagos ou cortesia',
    },
    { path: '/subscriptions', label: 'Assinaturas', caption: 'Receitas da conta Mercado Pago' },
    { path: '/payments', label: 'Pagamentos', caption: 'Contas a pagar e recebiveis' },
    { path: '/documents', label: 'Documentos', caption: 'Arquivos e contratos' },
    {
      path: '/knowledge-base',
      label: 'Base de Conhecimento',
      caption: 'Playbooks e notas internas',
    },
  ];
  readonly navigation = computed(() =>
    this.baseNavigation.filter(
      (item) =>
        !['/subscriptions', '/workspace-admin'].includes(item.path) ||
        this.platformSubscriptionStore.isBillingAdminTenant(),
    ),
  );
  readonly workspaceStatusCopy = computed(() => {
    if (!this.authStore.user()) {
      return 'Entre para liberar o workspace.';
    }

    return this.platformSubscriptionStore.subscription()?.status === 'ACTIVE'
      ? 'Plano ativo e pronto para operar.'
      : 'Acompanhe a assinatura do workspace em Meu plano.';
  });
  readonly activeNavigation = computed(() => {
    const currentUrl = this.currentUrl();
    return (
      this.navigation().find(
        (item) => currentUrl === item.path || currentUrl.startsWith(`${item.path}/`),
      ) ?? this.navigation()[0]
    );
  });

  constructor() {
    if (!this.authStore.isAuthenticated()) {
      this.authStore.ensureModalOpen();
    }

    effect(() => {
      const token = this.authStore.token();

      untracked(() => {
        if (token) {
          void this.bootstrapWorkspace();
          return;
        }

        this.resetAllWorkspaceData();
      });
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((value) => !value);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  logout(): void {
    this.authStore.logout();
    this.closeSidebar();
  }

  private async bootstrapWorkspace(): Promise<void> {
    await this.authStore.refreshMe();
    if (!this.authStore.isAuthenticated()) {
      return;
    }

    await this.platformSubscriptionStore.load();

    if (
      this.platformSubscriptionStore.isAccessRestricted() &&
      !this.platformSubscriptionStore.isBillingAdminTenant()
    ) {
      this.resetOperationalData();
      if (this.router.url !== '/workspace-plan') {
        await this.router.navigateByUrl('/workspace-plan');
      }
      return;
    }

    await Promise.allSettled([
      this.dashboardStore.load(),
      this.usersStore.load(),
      this.clientsStore.load(),
      this.projectsStore.load(),
      this.tasksStore.load(),
      this.paymentsStore.load(),
      this.documentsStore.load(),
      this.knowledgeBaseStore.load(),
    ]);
  }

  private resetOperationalData(): void {
    this.usersStore.reset();
    this.dashboardStore.reset();
    this.clientsStore.reset();
    this.projectsStore.reset();
    this.tasksStore.reset();
    this.paymentsStore.reset();
    this.platformBillingAdminStore.reset();
    this.documentsStore.reset();
    this.googleCalendarStore.reset();
    this.knowledgeBaseStore.reset();
  }

  private resetAllWorkspaceData(): void {
    this.resetOperationalData();
    this.platformSubscriptionStore.reset();
  }
}
