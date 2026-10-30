"use client";

import { ChangeEvent, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ChevronRight, FileText, Sparkles, UploadCloud } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ErrorState } from "@/components/ui/states";
import { InvoiceForm } from "@/components/invoices/invoice-form";
import { invoiceService } from "@/services/invoice-service";
import { ApiClientError } from "@/services/api-client";
import type { InvoiceItemRequest, InvoiceRequest, InvoiceUploadPreview } from "@/types/api";

const ACCEPTED = ["application/pdf", "image/jpeg", "image/png"];
const MAX_SIZE = 10 * 1024 * 1024;

export default function NewInvoicePage() {
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);
  const [mode, setMode] = useState<"ai" | "manual">("ai");
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<InvoiceUploadPreview | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function chooseFile(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0] ?? null;
    setError(null);
    if (!selected) return;
    if (!ACCEPTED.includes(selected.type) || selected.size === 0 || selected.size > MAX_SIZE) {
      setError("Choose a non-empty PDF, JPG, or PNG file up to 10 MB.");
      event.target.value = "";
      return;
    }
    setFile(selected);
  }

  async function extract() {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      setPreview(await invoiceService.uploadAndExtract(file));
    } catch (e) {
      setError(e instanceof ApiClientError ? e.details.message : "AI could not read this document. Please try another file.");
    } finally {
      setBusy(false);
    }
  }

  async function submit(request: InvoiceRequest) {
    setBusy(true);
    try {
      const invoice = await invoiceService.createInvoice(request);
      if (preview) {
        await Promise.all(preview.items.map((item): Promise<unknown> => {
          const line: InvoiceItemRequest = { description: item.description, quantity: item.quantity, unitPrice: item.unitPrice, amount: item.amount };
          return invoiceService.createInvoiceItem(invoice.id, line);
        }));
      }
      router.push(`/invoices/${invoice.id}`);
    } finally {
      setBusy(false);
    }
  }

  const initialRequest: InvoiceRequest | undefined = preview ? {
    invoiceNumber: preview.invoiceNumber,
    vendorName: preview.vendorName,
    invoiceDate: preview.invoiceDate,
    dueDate: preview.dueDate ?? "",
    status: "PENDING",
    subtotal: preview.subtotal,
    taxAmount: preview.taxAmount,
    totalAmount: preview.totalAmount,
    currency: preview.currency,
    notes: "",
    direction: "EXPENSE",
    fileUrl: preview.fileUrl,
  } : undefined;

  return (
    <section className="mx-auto max-w-4xl animate-fade-in">
      <div className="mb-2 flex items-center gap-2 text-sm font-medium text-teal-700"><FileText className="h-4 w-4" /><Link href="/invoices" className="hover:underline">Invoices</Link><ChevronRight className="h-4 w-4 text-slate-400" /><span className="text-slate-600">Create invoice</span></div>
      <h1 className="mt-1 text-2xl font-semibold text-slate-900">Create invoice</h1>
      <p className="mt-2 text-sm text-slate-600">Start with a document and review what AI found before saving it.</p>
      <div className="mt-6 flex gap-2 border-b border-slate-200">
        <button className={`border-b-2 px-3 py-2 text-sm font-medium ${mode === "ai" ? "border-teal-600 text-teal-700" : "border-transparent text-slate-500"}`} onClick={() => setMode("ai")} type="button">Upload with AI</button>
        <button className={`border-b-2 px-3 py-2 text-sm font-medium ${mode === "manual" ? "border-teal-600 text-teal-700" : "border-transparent text-slate-500"}`} onClick={() => setMode("manual")} type="button">Enter manually</button>
      </div>
      {error && <div className="mt-6"><ErrorState title="Unable to process invoice" description={error} /></div>}
      {mode === "ai" && !preview && <Card className="mt-6"><div className="rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 p-8 text-center"><Sparkles className="mx-auto h-10 w-10 text-indigo-500" /><h2 className="mt-4 font-semibold text-slate-900">Upload an invoice to extract it</h2><p className="mt-1 text-sm text-slate-500">PDF, JPG, or PNG up to 10 MB. Nothing is saved until you confirm.</p><input ref={inputRef} accept=".pdf,.jpg,.jpeg,.png" className="hidden" onChange={chooseFile} type="file" /><div className="mt-6 flex flex-col items-center gap-3">{file && <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-700">{file.name}</span>}<div className="flex gap-2"><Button disabled={busy} onClick={() => inputRef.current?.click()} type="button" variant="secondary">Choose document</Button>{file && <Button disabled={busy} onClick={() => void extract()} type="button" className="gap-2"><UploadCloud className="h-4 w-4" />{busy ? "Reading…" : "Extract with AI"}</Button>}</div></div></div></Card>}
      {mode === "ai" && preview && <><Card className="mt-6 border-indigo-100 bg-indigo-50/40"><div className="flex items-center gap-3"><Sparkles className="h-5 w-5 text-indigo-600" /><div><h2 className="font-semibold text-slate-900">Review AI extraction</h2><p className="text-sm text-slate-600">Edit the invoice fields below, then save when everything looks right.</p></div></div>{preview.items.length > 0 ? <div className="mt-4 overflow-x-auto rounded-lg border border-indigo-100 bg-white"><table className="w-full text-left text-sm"><thead className="border-b border-slate-200 bg-slate-50"><tr><th className="px-3 py-2">Description</th><th className="px-3 py-2">Qty</th><th className="px-3 py-2">Unit price</th><th className="px-3 py-2">Amount</th></tr></thead><tbody className="divide-y divide-slate-100">{preview.items.map((item, index) => <tr key={`${item.description}-${index}`}><td className="px-3 py-2 font-medium">{item.description}</td><td className="px-3 py-2">{item.quantity}</td><td className="px-3 py-2">{item.unitPrice.toFixed(2)}</td><td className="px-3 py-2">{item.amount.toFixed(2)}</td></tr>)}</tbody></table></div> : <p className="mt-4 text-sm text-slate-600">No line items were found in this document.</p>}</Card><div className="mt-6"><InvoiceForm initialRequest={initialRequest} saving={busy} onSubmit={submit} /></div></>}
      {mode === "manual" && <div className="mt-6"><InvoiceForm saving={busy} onSubmit={submit} /></div>}
    </section>
  );
}
