"use client";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Pencil, ChevronRight } from "lucide-react";
import { InvoiceForm } from "@/components/invoices/invoice-form";
import { ErrorState } from "@/components/ui/states";
import { Skeleton } from "@/components/ui/skeleton";
import { invoiceService } from "@/services/invoice-service";
import type { Invoice, InvoiceRequest } from "@/types/api";

export default function EditInvoicePage() {
  const id = String(useParams().id);
  const router = useRouter();
  const [inv, setInv] = useState<Invoice | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void invoiceService.getInvoice(id).then(setInv).catch(() => setError("Invoice could not be found."));
  }, [id]);

  if (error) return <ErrorState title="Invoice unavailable" description={error} />;
  if (!inv) return (
    <div className="mx-auto max-w-4xl space-y-4">
      <Skeleton className="h-8 w-64" />
      <Skeleton className="h-10 w-48" />
      <Skeleton className="h-96 w-full" />
    </div>
  );

  async function submit(data: InvoiceRequest) {
    setSaving(true);
    try {
      await invoiceService.updateInvoice(id, data);
      router.push(`/invoices/${id}`);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="mx-auto max-w-4xl animate-fade-in">
      <div className="mb-2 flex items-center gap-2 text-sm font-medium text-teal-700">
        <Pencil className="h-4 w-4" />
        <Link href="/invoices" className="hover:underline">Invoices</Link>
        <ChevronRight className="h-4 w-4 text-slate-400" />
        <Link href={`/invoices/${id}`} className="hover:underline">{inv.invoiceNumber}</Link>
        <ChevronRight className="h-4 w-4 text-slate-400" />
        <span className="text-slate-600">Edit invoice</span>
      </div>
      <h1 className="mt-1 text-2xl font-semibold text-slate-900">Edit invoice</h1>
      <div className="mt-6">
        <InvoiceForm initial={inv} saving={saving} onSubmit={submit} />
      </div>
    </section>
  );
}
