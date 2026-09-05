export type UserProfile = {
  id: string;
  firstName: string;
  lastName: string;
  companyName: string | null;
  email: string;
  phoneNumber: string | null;
  country: string | null;
  createdAt: string;
  updatedAt: string;
};

export type LoginResponse = {
  token: string;
  user: UserProfile;
};

export type ApiErrorResponse = {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
};

export type InvoiceStatus = "PENDING" | "PAID" | "OVERDUE";
export type InvoiceDirection = "INCOME" | "EXPENSE";

export type Invoice = {
  id: string;
  userId: string;
  categoryId: string | null;
  invoiceNumber: string;
  vendorName: string;
  invoiceDate: string;
  dueDate: string | null;
  status: InvoiceStatus;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  currency: string;
  notes: string | null;
  fileUrl: string | null;
  createdAt: string;
  updatedAt: string;
  direction: InvoiceDirection;
};

export type InvoiceRequest = {
  categoryId?: string;
  invoiceNumber: string;
  vendorName: string;
  invoiceDate: string;
  dueDate?: string;
  status: InvoiceStatus;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  currency: string;
  notes?: string;
  direction: InvoiceDirection;
};

export type InvoiceItem = {
  id: string;
  invoiceId: string;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
};

export type InvoiceItemRequest = {
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
};

export type Category = {
  id: string;
  userId: string;
  name: string;
  description: string | null;
  createdAt: string;
};

export type DocumentResponse = {
  invoiceId: string;
  url: string;
};

export type DashboardCurrencySummary = {
  currency: string;
  revenue: number;
  expenses: number;
  paidInvoices: number;
  pendingInvoices: number;
  invoiceCount: number;
};

export type DashboardSummaryResponse = {
  summaries: DashboardCurrencySummary[];
};

export type MonthlyAnalyticsEntry = {
  month: string;
  currency: string;
  revenue: number;
  expenses: number;
  invoiceCount: number;
};

export type MonthlyAnalyticsResponse = { months: MonthlyAnalyticsEntry[] };

export type CategoryAnalyticsEntry = {
  categoryId: string | null;
  categoryName: string;
  direction: InvoiceDirection;
  currency: string;
  totalAmount: number;
  invoiceCount: number;
};

export type CategoryAnalyticsResponse = { categories: CategoryAnalyticsEntry[] };

export type RevenueAnalyticsEntry = {
  month: string;
  currency: string;
  revenue: number;
  invoiceCount: number;
};

export type RevenueAnalyticsResponse = { trends: RevenueAnalyticsEntry[] };
