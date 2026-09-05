"use client";
import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Pencil, Trash2, ArrowLeft, Calendar, CircleDollarSign, Tag, FileText, TrendingUp, TrendingDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { DocumentSection } from "@/components/invoices/document-section";
import { getAuthErrorMessage } from "@/lib/auth-errors";
import { invoiceService } from "@/services/invoice-service";
import type { Invoice, InvoiceItem, InvoiceItemRequest, InvoiceStatus, InvoiceDirection } from "@/types/api";

function StatusBadge({ status }: { status: InvoiceStatus }) {
  if (status === "PAID") return <Badge variant="green">PAID</Badge>;
  if (status === "PENDING") return <Badge variant="amber">PENDING</Badge>;
  if (status === "OVERDUE") return <Badge variant="red">OVERDUE</Badge>;
  return <Badge variant="slate">{status}</Badge>;
}

function DirectionBadge({ direction }: { direction: InvoiceDirection }) {
  if (direction === "INCOME") {
    return <Badge variant="teal" className="gap-1"><TrendingUp className="h-3 w-3" /> INCOME</Badge>;
  }
  if (direction === "EXPENSE") {
    return <Badge variant="red" className="gap-1"><TrendingDown className="h-3 w-3" /> EXPENSE</Badge>;
  }
  return <Badge variant="slate">{direction}</Badge>;
}

export default function InvoiceDetailPage() {
  const id = String(useParams().id);
  const router = useRouter();
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [items, setItems] = useState<InvoiceItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [itemError, setItemError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [status, setStatus] = useState<InvoiceStatus>("PENDING");
  const [editingItem, setEditingItem] = useState<InvoiceItem | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [form, setForm] = useState<InvoiceItemRequest>({ description: "", quantity: 1, unitPrice: 0, amount: 0 });

  const load = useCallback(async () => {
    try {
      const value = await invoiceService.getInvoice(id);
      setInvoice(value);
      setStatus(value.status);
      setItems(await invoiceService.getInvoiceItems(id));
    } catch (e) {
      setError(getAuthErrorMessage(e));
    }
  }, [id]);

  useEffect(() => { void load(); }, [load]);

  async function changeStatus(value: InvoiceStatus) {
    setPending(true);
    try {
      const value2 = await invoiceService.updateStatus(id, value);
      setInvoice(value2);
      setStatus(value2.status);
    } catch (e) {
      setError(getAuthErrorMessage(e));
    } finally {
      setPending(false);
    }
  }

  async function saveItem(event: FormEvent) {
    event.preventDefault();
    setItemError(null);
    if (!form.description.trim() || form.quantity <= 0 || form.unitPrice < 0 || form.amount < 0) {
      setItemError("Enter valid line-item values.");
      return;
    }
    setPending(true);
    try {
      const saved = editingItem ? await invoiceService.updateInvoiceItem(id, editingItem.id, form) : await invoiceService.createInvoiceItem(id, form);
      setItems((current) => editingItem ? current.map((item) => item.id === saved.id ? saved : item) : [...current, saved]);
      setEditingItem(null);
      setForm({ description: "", quantity: 1, unitPrice: 0, amount: 0 });
    } catch (e) {
      setItemError(getAuthErrorMessage(e));
    } finally {
      setPending(false);
    }
  }

  async function removeItem(item: InvoiceItem) {
    if (!window.confirm(`Delete ${item.description}?`)) return;
    setPending(true);
    try {
      await invoiceService.deleteInvoiceItem(id, item.id);
      setItems((current) => current.filter((value) => value.id !== item.id));
    } catch (e) {
      setItemError(getAuthErrorMessage(e));
    } finally {
      setPending(false);
    }
  }

  async function removeInvoice() {
    setPending(true);
    try {
      await invoiceService.deleteInvoice(id);
      router.push("/invoices");
    } catch (e) {
      setError(getAuthErrorMessage(e));
      setPending(false);
      setConfirmDelete(false);
    }
  }

  if (error) return <ErrorState title="Invoice unavailable" description="This invoice could not be loaded or accessed." />;
  if (!invoice) return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-10 w-96" />
      </div>
      <Skeleton className="h-64 w-full" />
    </div>
  );

  return (
    <section className="mx-auto max-w-5xl animate-fade-in">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <p className="text-sm font-medium text-teal-700">Invoice</p>
            <DirectionBadge direction={invoice.direction} />
            <StatusBadge status={invoice.status} />
          </div>
          <h1 className="mt-2 text-3xl font-bold text-slate-900">{invoice.invoiceNumber}</h1>
          <p className="mt-1 text-lg text-slate-600">{invoice.vendorName}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link href={`/invoices/${id}/edit`}>
            <Button variant="secondary" className="gap-2"><Pencil className="h-4 w-4" /> Edit</Button>
          </Link>
          <Button variant="danger" className="gap-2" onClick={() => setConfirmDelete(true)}>
            <Trash2 className="h-4 w-4" /> Delete
          </Button>
          <Button variant="ghost" className="gap-2" onClick={() => router.push("/invoices")}>
            <ArrowLeft className="h-4 w-4" /> Back
          </Button>
        </div>
      </div>

      <Card className="mt-6">
        <div className="grid gap-6 sm:grid-cols-2 md:grid-cols-3">
          <div className="flex items-start gap-3">
            <Calendar className="h-5 w-5 text-slate-400 mt-0.5" />
            <div>
              <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Invoice Date</dt>
              <dd className="mt-1 text-sm font-medium text-slate-900">{invoice.invoiceDate}</dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Calendar className="h-5 w-5 text-slate-400 mt-0.5" />
            <div>
              <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Due Date</dt>
              <dd className="mt-1 text-sm font-medium text-slate-900">{invoice.dueDate || "—"}</dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Tag className="h-5 w-5 text-slate-400 mt-0.5" />
            <div>
              <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Category</dt>
              <dd className="mt-1 text-sm font-medium text-slate-900">{invoice.categoryId || "Uncategorized"}</dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <CircleDollarSign className="h-5 w-5 text-slate-400 mt-0.5" />
            <div>
              <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Subtotal</dt>
              <dd className="mt-1 text-sm font-medium text-slate-900">{invoice.currency} {invoice.subtotal.toFixed(2)}</dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <CircleDollarSign className="h-5 w-5 text-slate-400 mt-0.5" />
            <div>
              <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Tax</dt>
              <dd className="mt-1 text-sm font-medium text-slate-900">{invoice.currency} {invoice.taxAmount.toFixed(2)}</dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <CircleDollarSign className="h-5 w-5 text-teal-600 mt-0.5" />
            <div>
              <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total</dt>
              <dd className="mt-1 text-lg font-bold text-teal-700">{invoice.currency} {invoice.totalAmount.toFixed(2)}</dd>
            </div>
          </div>
          <div className="sm:col-span-2 md:col-span-3 flex items-start gap-3 border-t border-slate-100 pt-4">
            <FileText className="h-5 w-5 text-slate-400 mt-0.5" />
            <div>
              <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Notes</dt>
              <dd className="mt-1 text-sm text-slate-700 whitespace-pre-wrap">{invoice.notes || "—"}</dd>
            </div>
          </div>
        </div>
        <div className="mt-6 flex items-center gap-3 border-t border-slate-100 pt-4">
          <Label htmlFor="invoice-status" className="font-medium">Update Status</Label>
          <select 
            className="min-h-10 rounded-lg border border-slate-300 bg-white px-3 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500" 
            id="invoice-status" 
            value={status} 
            onChange={(event) => void changeStatus(event.target.value as InvoiceStatus)} 
            disabled={pending}
          >
            {["PENDING", "PAID", "OVERDUE"].map((value) => <option key={value}>{value}</option>)}
          </select>
        </div>
      </Card>

      <DocumentSection 
        invoice={invoice} 
        onInvoiceUpdate={(updated) => { setInvoice(updated); setStatus(updated.status); }} 
        onItemsUpdate={setItems} 
      />

      <Card className="mt-6">
        <h2 className="text-lg font-semibold text-slate-900">Line Items</h2>
        {itemError && <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-800">{itemError}</p>}
        
        {items.length === 0 ? (
          <div className="mt-4">
            <EmptyState title="No invoice items" description="Add line items below." />
          </div>
        ) : (
          <div className="mt-4 overflow-hidden rounded-lg border border-slate-200">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600">
                <tr>
                  <th className="px-4 py-3 font-semibold">Description</th>
                  <th className="px-4 py-3 font-semibold">Qty</th>
                  <th className="px-4 py-3 font-semibold">Unit Price</th>
                  <th className="px-4 py-3 font-semibold">Amount</th>
                  <th className="px-4 py-3 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {items.map((item) => (
                  <tr key={item.id} className="transition-colors hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium text-slate-900">{item.description}</td>
                    <td className="px-4 py-3 text-slate-600">{item.quantity}</td>
                    <td className="px-4 py-3 text-slate-600">{invoice.currency} {item.unitPrice.toFixed(2)}</td>
                    <td className="px-4 py-3 font-medium text-slate-900">{invoice.currency} {item.amount.toFixed(2)}</td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <button 
                          aria-label="Edit item"
                          className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-teal-700 transition-colors" 
                          onClick={() => { setEditingItem(item); setForm({ description: item.description, quantity: item.quantity, unitPrice: item.unitPrice, amount: item.amount }); }} 
                          type="button"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button 
                          aria-label="Delete item"
                          className="rounded p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-700 transition-colors" 
                          onClick={() => void removeItem(item)} 
                          type="button"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="mt-6 rounded-lg bg-slate-50 p-5 border border-slate-100">
          <h3 className="text-sm font-semibold text-slate-800 mb-4">{editingItem ? "Edit line item" : "Add line item"}</h3>
          <form className="grid gap-4 sm:grid-cols-4" onSubmit={saveItem}>
            <div className="sm:col-span-2">
              <Label htmlFor="item-description">Description</Label>
              <Input id="item-description" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
            </div>
            <div>
              <Label htmlFor="item-quantity">Quantity</Label>
              <Input id="item-quantity" min="0.001" step="0.001" type="number" value={form.quantity} onChange={(event) => setForm({ ...form, quantity: Number(event.target.value) })} />
            </div>
            <div>
              <Label htmlFor="item-unit">Unit price</Label>
              <Input id="item-unit" min="0" step="0.01" type="number" value={form.unitPrice} onChange={(event) => setForm({ ...form, unitPrice: Number(event.target.value) })} />
            </div>
            <div className="sm:col-span-2">
              <Label htmlFor="item-amount">Total Amount</Label>
              <Input id="item-amount" min="0" step="0.01" type="number" value={form.amount} onChange={(event) => setForm({ ...form, amount: Number(event.target.value) })} />
            </div>
            <div className="sm:col-span-4 flex items-center gap-3 pt-2">
              <Button disabled={pending} type="submit" className="gap-2">
                {pending ? "Saving…" : editingItem ? <><Pencil className="h-4 w-4"/> Save item</> : <><Pencil className="h-4 w-4"/> Add item</>}
              </Button>
              {editingItem && (
                <Button variant="secondary" onClick={() => { setEditingItem(null); setForm({ description: "", quantity: 1, unitPrice: 0, amount: 0 }); }}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </div>
      </Card>

      <Dialog open={confirmDelete} title="Delete invoice" onClose={() => !pending && setConfirmDelete(false)}>
        <p className="text-sm text-slate-600">Delete <strong>{invoice.invoiceNumber}</strong>? This cannot be undone.</p>
        <div className="mt-5 flex justify-end gap-3">
          <Button variant="secondary" disabled={pending} onClick={() => setConfirmDelete(false)}>Cancel</Button>
          <Button variant="danger" disabled={pending} onClick={() => void removeInvoice()}>{pending ? "Deleting…" : "Delete invoice"}</Button>
        </div>
      </Dialog>
    </section>
  );
}
