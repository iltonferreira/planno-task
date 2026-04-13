import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthStore } from '../../core/stores/auth.store';
import { DashboardStore } from '../../core/stores/dashboard.store';
import { TasksStore } from '../../core/stores/tasks.store';
import { UsersStore } from '../../core/stores/users.store';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss'
})
export class DashboardPageComponent {
  private readonly authStore = inject(AuthStore);
  readonly dashboardStore = inject(DashboardStore);
  private readonly tasksStore = inject(TasksStore);
  private readonly usersStore = inject(UsersStore);

  readonly userName = computed(() => this.authStore.user()?.name.split(' ')[0] ?? 'Equipe');
  readonly summary = computed(() => this.dashboardStore.summary());
  readonly clientsCount = computed(() => this.summary()?.totalClients ?? 0);
  readonly activeProjects = computed(() => this.summary()?.activeProjects ?? 0);
  readonly recurringRevenue = computed(() => this.summary()?.recurringRevenue ?? 0);
  readonly overdueRevenue = computed(() => this.summary()?.overdueSubscriptions ?? 0);
  readonly approvedIncome = computed(() => this.summary()?.approvedIncome ?? 0);
  readonly approvedExpenses = computed(() => this.summary()?.approvedExpenses ?? 0);
  readonly pendingIncome = computed(() => this.summary()?.pendingIncome ?? 0);
  readonly pendingExpenses = computed(() => this.summary()?.pendingExpenses ?? 0);
  readonly netCashflow = computed(() => this.summary()?.netCashflow ?? 0);
  readonly knowledgeBaseEntries = computed(() => this.summary()?.knowledgeBaseEntries ?? 0);

  readonly taskDistribution = computed(() => {
    const entries = this.summary()?.tasksByStatus ?? [];
    const max = Math.max(1, ...entries.map((entry) => entry.value));

    return entries.map((entry) => {
      const count = entry.value;
      return {
        status: entry.label,
        count,
        width: `${(count / max) * 100}%`
      };
    });
  });

  readonly revenueLeaders = computed(() => {
    const clients = this.summary()?.topClients ?? [];
    const max = Math.max(1, ...clients.map((client) => client.value || 0));

    return clients.map((client) => ({
      ...client,
      width: `${((client.value || 0) / max) * 100}%`
    }));
  });

  readonly revenueByMonth = computed(() => {
    const entries = this.summary()?.revenueByMonth ?? [];
    const max = Math.max(1, ...entries.map((entry) => entry.value || 0));

    return entries.map((entry) => ({
      ...entry,
      height: `${Math.max(8, ((entry.value || 0) / max) * 100)}%`
    }));
  });

  readonly paymentBreakdown = computed(() => this.summary()?.paymentsByStatus ?? []);

  readonly workloadHighlights = computed(() => {
    const tasks = this.tasksStore.items();
    const users = this.usersStore.items();
    const entries = users
      .map((user) => ({
        user,
        total: tasks.filter((task) => task.responsibleUser?.id === user.id).length
      }))
      .filter((entry) => entry.total > 0)
      .sort((left, right) => right.total - left.total)
      .slice(0, 4);

    const max = Math.max(1, ...entries.map((entry) => entry.total));
    return entries.map((entry) => ({
      ...entry,
      width: `${(entry.total / max) * 100}%`
    }));
  });

  readonly focusTasks = computed(() =>
    [...this.tasksStore.items()]
      .filter((task) => Boolean(task.dueDate) && !['DONE', 'CANCELLED'].includes(task.status))
      .sort((left, right) => new Date(left.dueDate ?? '').getTime() - new Date(right.dueDate ?? '').getTime())
      .slice(0, 5)
  );
}
