import { Routes } from '@angular/router';

import { BillingAdminPageComponent } from './pages/billing-admin/billing-admin.page';
import { CalendarPageComponent } from './pages/calendar/calendar.page';
import { ClientsPageComponent } from './pages/clients/clients.page';
import { DashboardPageComponent } from './pages/dashboard/dashboard.page';
import { DocumentsPageComponent } from './pages/documents/documents.page';
import { KnowledgeBasePageComponent } from './pages/knowledge-base/knowledge-base.page';
import { PaymentsPageComponent } from './pages/payments/payments.page';
import { ProjectsPageComponent } from './pages/projects/projects.page';
import { TasksPageComponent } from './pages/tasks/tasks.page';
import { WorkspaceAdminPageComponent } from './pages/workspace-admin/workspace-admin.page';
import { WorkspacePlanPageComponent } from './pages/workspace-plan/workspace-plan.page';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'dashboard',
    component: DashboardPageComponent
  },
  {
    path: 'clients',
    component: ClientsPageComponent
  },
  {
    path: 'projects',
    component: ProjectsPageComponent
  },
  {
    path: 'tasks',
    component: TasksPageComponent
  },
  {
    path: 'calendar',
    component: CalendarPageComponent
  },
  {
    path: 'subscriptions',
    component: BillingAdminPageComponent
  },
  {
    path: 'workspace-admin',
    component: WorkspaceAdminPageComponent
  },
  {
    path: 'payments',
    component: PaymentsPageComponent
  },
  {
    path: 'workspace-plan',
    component: WorkspacePlanPageComponent
  },
  {
    path: 'documents',
    component: DocumentsPageComponent
  },
  {
    path: 'knowledge-base',
    component: KnowledgeBasePageComponent
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
