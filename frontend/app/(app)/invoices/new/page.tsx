"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import Link from "next/link";
import { FileText, ChevronRight } from "lucide-react";
import { InvoiceForm } from "@/components/invoices/invoice-form";
import { invoiceService } from "@/services/invoice-service";
import type { InvoiceRequest } from "@/types/api";

export default function NewInvoicePage() {
  const router = useRouter();
  const [saving, setSaving] = useState(false);

  async function submit(data: InvoiceRequest) {
    setSaving(true);
    try {
      const invoice = await invoiceService.createInvoice(data);
      router.push(`/invoices/${invoice.id}`);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="mx-auto max-w-4xl animate-fade-in">
      <div className="mb-2 flex items-center gap-2 text-sm font-medium text-teal-700">
        <FileText className="h-4 w-4" />
        <Link href="/invoices" className="hover:underline">Invoices</Link>
        <ChevronRight className="h-4 w-4 text-slate-400" />
        <span className="text-slate-600">Create invoice</span>
      </div>
      <h1 className="mt-1 text-2xl font-semibold text-slate-900">Create invoice</h1>
      <div className="mt-6">
        <InvoiceForm saving={saving} onSubmit={submit} />
      </div>
    </section>
  );
}
