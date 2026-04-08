export type ThemeMode = 'light' | 'dark';

export interface UserSummary {
  id: number;
  name: string;
  email: string;
}

export interface User extends UserSummary {
  cpf: string;
  tenantId: number;
  tenantName: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Client {
  id: number;
  name: string;
  email: string;
  phone: string;
  document: string;
  type: string;
  totalValue: number;
  active: boolean;
}

export interface Project {
  id: number;
  name: string;
  description: string;
  status: string;
  budget: number | null;
  startDate: string | null;
  endDate: string | null;
  clientId: number | null;
  clientName: string | null;
  ownerUser: UserSummary | null;
  createdBy: UserSummary | null;
  createdAt: string;
  updatedAt: string;
}

export interface Task {
  id: number;
  title: string;
  description: string;
  status: string;
  priority: string;
  dueDate: string | null;
  startAt: string | null;
  endAt: string | null;
  allDay: boolean;
  positionIndex: number;
  projectId: number | null;
  projectName: string | null;
  responsibleUser: UserSummary | null;
  createdBy: UserSummary | null;
  participants: UserSummary[];
  createdAt: string;
  updatedAt: string;
}

export interface Subscription {
  id: number;
  description: string;
  price: number;
  status: string;
  nextBillingDate: string | null;
  externalReference: string | null;
  externalSubscriptionId: string | null;
  checkoutUrl: string | null;
  clientId: number;
  clientName: string;
}

export type TenantBillingMode = 'SUBSCRIPTION_REQUIRED' | 'COMPLIMENTARY';

export interface PlatformSubscription {
  id: number | null;
  planCode: string;
  planName: string;
  amount: number;
  currencyId: string;
  status: string;
  payerEmail: string | null;
  externalReference: string | null;
  externalSubscriptionId: string | null;
  checkoutUrl: string | null;
  nextBillingDate: string | null;
  lastPaymentAt: string | null;
  requiresAction: boolean;
  billingAdminTenant: boolean;
  billingMode: TenantBillingMode;
}

export interface PlatformBillingCustomer {
  tenantId: number;
  tenantName: string;
  tenantSlug: string;
  tenantActive: boolean;
  billingMode: TenantBillingMode;
  planName: string;
  amount: number;
  currencyId: string;
  status: string;
  adminName: string | null;
  adminEmail: string | null;
  payerEmail: string | null;
  externalReference: string | null;
  externalSubscriptionId: string | null;
  checkoutUrl: string | null;
  nextBillingDate: string | null;
  lastPaymentAt: string | null;
}

export interface MercadoPagoSubscriptionItem {
  id: string | null;
  reason: string | null;
  status: string | null;
  payerEmail: string | null;
  externalReference: string | null;
  initPoint: string | null;
  amount: number | null;
  currencyId: string | null;
  nextPaymentDate: string | null;
  linkedTenantId: number | null;
  linkedTenantName: string | null;
}

export interface PlatformBillingAdminOverview {
  adminTenantName: string;
  totalCustomers: number;
  activeCustomers: number;
  projectedMonthlyRevenue: number;
  pendingMonthlyRevenue: number;
  mercadoPagoSubscriptionCount: number;
  mercadoPagoActiveSubscriptionCount: number;
  mercadoPagoProjectedRevenue: number;
  customers: PlatformBillingCustomer[];
  mercadoPagoSubscriptions: MercadoPagoSubscriptionItem[];
}

export interface PlatformPaymentLinkResult {
  externalReference: string;
  preferenceId: string | null;
  checkoutUrl: string | null;
}

export interface PlatformWorkspaceProvisionResult {
  tenantId: number;
  tenantName: string;
  tenantSlug: string;
  billingMode: TenantBillingMode;
  adminUserId: number;
  adminName: string;
  adminEmail: string;
  platformStatus: string;
  checkoutUrl: string | null;
}

export interface Payment {
  id: number;
  title: string;
  description: string | null;
  amount: number;
  type: string;
  direction: string;
  status: string;
  provider: string;
  dueDate: string | null;
  paidAt: string | null;
  clientId: number | null;
  clientName: string | null;
  projectId: number | null;
  projectName: string | null;
  subscriptionId: number | null;
  subscriptionName: string | null;
  createdBy: UserSummary | null;
  externalReference: string | null;
  externalPaymentId: string | null;
  externalPreferenceId: string | null;
  externalSubscriptionId: string | null;
  checkoutUrl: string | null;
  statusDetail: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentAsset {
  id: number;
  name: string;
  mimeType: string;
  fileSize: number | null;
  storageFileId: string;
  storageFolderId: string | null;
  storageFolderPath: string;
  webViewUrl: string | null;
  relationType: string;
  relationId: number | null;
  uploadedBy: UserSummary | null;
  createdAt: string;
}

export interface GoogleDriveConnectionStatus {
  enabled: boolean;
  configured: boolean;
  connected: boolean;
  googleAccountEmail: string | null;
  rootFolderId: string | null;
  expiresAt: string | null;
}

export interface GoogleCalendarConnectionStatus {
  enabled: boolean;
  configured: boolean;
  connected: boolean;
  googleAccountEmail: string | null;
  defaultCalendarId: string | null;
  expiresAt: string | null;
}

export interface GoogleCalendarEvent {
  calendarId: string;
  eventId: string;
  summary: string;
  description: string | null;
  status: string | null;
  htmlLink: string | null;
  allDay: boolean;
  startDate: string | null;
  endDate: string | null;
  startAt: string | null;
  endAt: string | null;
  linkedTaskId: number | null;
}

export interface KnowledgeBasePage {
  id: number;
  title: string;
  slug: string;
  summary: string | null;
  content: string;
  pinned: boolean;
  wordCount: number;
  createdBy: UserSummary | null;
  updatedBy: UserSummary | null;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardMetricValue {
  label: string;
  value: number;
}

export interface DashboardClientValue {
  clientId: number;
  clientName: string;
  value: number;
}

export interface DashboardSummary {
  totalClients: number;
  activeProjects: number;
  openTasks: number;
  knowledgeBaseEntries: number;
  recurringRevenue: number;
  approvedIncome: number;
  approvedExpenses: number;
  pendingIncome: number;
  pendingExpenses: number;
  overdueSubscriptions: number;
  netCashflow: number;
  revenueByMonth: DashboardMetricValue[];
  paymentsByStatus: DashboardMetricValue[];
  topClients: DashboardClientValue[];
  tasksByStatus: DashboardMetricValue[];
}
