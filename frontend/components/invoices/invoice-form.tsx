"use client";

import { FormEvent, useEffect, useState } from "react";
import { Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ErrorState } from "@/components/ui/states";
import { getAuthErrorMessage } from "@/lib/auth-errors";
import { categoryService } from "@/services/category-service";
import type { Category, Invoice, InvoiceDirection, InvoiceRequest, InvoiceStatus } from "@/types/api";

type Props = { initial?: Invoice; initialRequest?: InvoiceRequest; saving: boolean; onSubmit: (request: InvoiceRequest) => Promise<void> };
const blank: InvoiceRequest = { invoiceNumber: "", vendorName: "", invoiceDate: "", dueDate: "", status: "PENDING", subtotal: 0, taxAmount: 0, totalAmount: 0, currency: "INR", notes: "", direction: "EXPENSE" };

export function InvoiceForm({ initial, initialRequest, saving, onSubmit }: Props) {
  const [form, setForm] = useState<InvoiceRequest>(blank);
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => { 
    if (initial) setForm({ categoryId: initial.categoryId ?? undefined, invoiceNumber: initial.invoiceNumber, vendorName: initial.vendorName, invoiceDate: initial.invoiceDate, dueDate: initial.dueDate ?? "", status: initial.status, subtotal: initial.subtotal, taxAmount: initial.taxAmount, totalAmount: initial.totalAmount, currency: initial.currency, notes: initial.notes ?? "", direction: initial.direction, fileUrl: initial.fileUrl ?? undefined });
    else if (initialRequest) setForm(initialRequest);
  }, [initial, initialRequest]);

  useEffect(() => { 
    void categoryService.getCategories().then(setCategories).catch(() => setCategories([])); 
  }, []);

  const set = (key: keyof InvoiceRequest, value: string | number) => setForm((current) => ({ ...current, [key]: value }));

  async function submit(event: FormEvent<HTMLFormElement>) { 
    event.preventDefault(); 
    setError(null); 
    if (!form.invoiceNumber.trim() || !form.vendorName.trim() || !form.invoiceDate || !form.currency.match(/^[A-Z]{3}$/)) { 
      setError("Complete the required fields and use a three-letter uppercase currency code."); 
      return; 
    } 
    try { 
      await onSubmit({ ...form, invoiceNumber: form.invoiceNumber.trim(), vendorName: form.vendorName.trim(), currency: form.currency.toUpperCase(), dueDate: form.dueDate || undefined, notes: form.notes || undefined }); 
    } catch (e) { 
      setError(getAuthErrorMessage(e)); 
    } 
  }

  return (
    <Card>
      <form className="space-y-8" onSubmit={submit} noValidate>
        {/* Invoice Details Section */}
        <div>
          <h2 className="text-sm font-semibold text-slate-900 border-b border-slate-200 pb-2 mb-4">Invoice Details</h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label="Invoice number" value={form.invoiceNumber} required onChange={(v) => set("invoiceNumber", v)} />
            <Field label="Vendor name" value={form.vendorName} required onChange={(v) => set("vendorName", v)} />
            <Field label="Invoice date" type="date" value={form.invoiceDate} required onChange={(v) => set("invoiceDate", v)} />
            <Field label="Due date" type="date" value={form.dueDate ?? ""} onChange={(v) => set("dueDate", v)} />
            <Select label="Status" value={form.status} options={["PENDING", "PAID", "OVERDUE"]} onChange={(v) => set("status", v as InvoiceStatus)} />
            <Select label="Direction" value={form.direction} options={["INCOME", "EXPENSE"]} onChange={(v) => set("direction", v as InvoiceDirection)} />
          </div>
        </div>

        {/* Financial Details Section */}
        <div>
          <h2 className="text-sm font-semibold text-slate-900 border-b border-slate-200 pb-2 mb-4">Financial Details</h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label="Subtotal" type="number" step="0.01" value={String(form.subtotal)} onChange={(v) => set("subtotal", Number(v))} />
            <Field label="Tax amount" type="number" step="0.01" value={String(form.taxAmount)} onChange={(v) => set("taxAmount", Number(v))} />
            <Field label="Total amount" type="number" step="0.01" value={String(form.totalAmount)} onChange={(v) => set("totalAmount", Number(v))} />
            <Field label="Currency" maxLength={3} value={form.currency} required onChange={(v) => set("currency", v.toUpperCase())} />
          </div>
        </div>

        {/* Additional Information Section */}
        <div>
          <h2 className="text-sm font-semibold text-slate-900 border-b border-slate-200 pb-2 mb-4">Additional Information</h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="invoice-category">Category</Label>
              <select className="min-h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500" id="invoice-category" onChange={(e) => setForm((c) => ({ ...c, categoryId: e.target.value || undefined }))} value={form.categoryId ?? ""}>
                <option value="">No category</option>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="invoice-notes">Notes</Label>
              <textarea className="min-h-24 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500" id="invoice-notes" maxLength={2000} onChange={(e) => set("notes", e.target.value)} value={form.notes ?? ""} />
            </div>
          </div>
        </div>

        {error && (
          <div>
            <ErrorState title="Unable to save invoice" description={error} />
          </div>
        )}

        <div className="flex justify-end pt-4 border-t border-slate-100">
          <Button disabled={saving} type="submit" className="gap-2">
            <Save className="h-4 w-4" />
            {saving ? "Saving…" : initial ? "Save changes" : "Create invoice"}
          </Button>
        </div>
      </form>
    </Card>
  );
}

function Field({ label, value, onChange, type = "text", required, step, maxLength }: { label: string; value: string; onChange: (value: string) => void; type?: string; required?: boolean; step?: string; maxLength?: number }) { 
  const id = `invoice-${label.toLowerCase().replaceAll(" ", "-")}`; 
  return (
    <div className="space-y-2">
      <Label htmlFor={id} className="flex gap-1 items-center">
        {label} 
        {required && <span className="text-red-500">*</span>}
      </Label>
      <Input id={id} maxLength={maxLength} onChange={(e) => onChange(e.target.value)} required={required} step={step} type={type} value={value} />
    </div>
  ); 
}

function Select({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (value: string) => void }) { 
  const id = `invoice-${label.toLowerCase()}`; 
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <select className="min-h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500" id={id} onChange={(e) => onChange(e.target.value)} value={value}>
        {options.map((option) => <option key={option}>{option}</option>)}
      </select>
    </div>
  ); 
}
