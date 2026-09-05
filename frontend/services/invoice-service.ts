import { apiClient } from "@/services/api-client";
import type { DocumentResponse, Invoice, InvoiceItem, InvoiceItemRequest, InvoiceRequest, InvoiceStatus } from "@/types/api";

export const invoiceService = {
  getInvoices: () => apiClient.get<Invoice[]>("/api/invoices"),
  getInvoice: (invoiceId: string) => apiClient.get<Invoice>(`/api/invoices/${invoiceId}`),
  createInvoice: (request: InvoiceRequest) => apiClient.post<Invoice>("/api/invoices", request),
  updateInvoice: (invoiceId: string, request: InvoiceRequest) => apiClient.put<Invoice>(`/api/invoices/${invoiceId}`, request),
  deleteInvoice: (invoiceId: string) => apiClient.delete(`/api/invoices/${invoiceId}`),
  updateStatus: (invoiceId: string, status: InvoiceStatus) => apiClient.patch<Invoice>(`/api/invoices/${invoiceId}/status`, { status }),
  getInvoiceItems: (invoiceId: string) => apiClient.get<InvoiceItem[]>(`/api/invoices/${invoiceId}/items`),
  createInvoiceItem: (invoiceId: string, request: InvoiceItemRequest) => apiClient.post<InvoiceItem>(`/api/invoices/${invoiceId}/items`, request),
  getInvoiceItem: (invoiceId: string, itemId: string) => apiClient.get<InvoiceItem>(`/api/invoices/${invoiceId}/items/${itemId}`),
  updateInvoiceItem: (invoiceId: string, itemId: string, request: InvoiceItemRequest) => apiClient.put<InvoiceItem>(`/api/invoices/${invoiceId}/items/${itemId}`, request),
  deleteInvoiceItem: (invoiceId: string, itemId: string) => apiClient.delete(`/api/invoices/${invoiceId}/items/${itemId}`),
  uploadDocument: (invoiceId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient.upload<Invoice>(`/api/invoices/${invoiceId}/document`, formData);
  },
  getDocument: (invoiceId: string) => apiClient.get<DocumentResponse>(`/api/invoices/${invoiceId}/document`),
  deleteDocument: (invoiceId: string) => apiClient.delete(`/api/invoices/${invoiceId}/document`),
  extract: (invoiceId: string) => apiClient.post<Invoice>(`/api/invoices/${invoiceId}/extract`),
};
