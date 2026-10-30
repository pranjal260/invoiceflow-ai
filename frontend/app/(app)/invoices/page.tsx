"use client";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { FileText, Plus, Search, Pencil, Trash2, TrendingUp, TrendingDown, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { getAuthErrorMessage } from "@/lib/auth-errors";
import { invoiceService } from "@/services/invoice-service";
import type { Invoice, InvoiceStatus, InvoiceDirection } from "@/types/api";

export default function InvoicesPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | InvoiceStatus>("ALL");
  const [directionFilter, setDirectionFilter] = useState<"ALL" | InvoiceDirection>("ALL");
  const [deleting, setDeleting] = useState<Invoice | null>(null);
  const [pending, setPending] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setInvoices(await invoiceService.getInvoices());
      setError(null);
    } catch (e) {
      setError(getAuthErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = invoices.filter(i =>
    `${i.invoiceNumber} ${i.vendorName}`.toLowerCase().includes(query.toLowerCase())
    && (statusFilter === "ALL" || i.status === statusFilter)
    && (directionFilter === "ALL" || i.direction === directionFilter)
  );

  async function remove() {
    if (!deleting) return;
    setPending(true);
    try {
      await invoiceService.deleteInvoice(deleting.id);
      setInvoices(c => c.filter(i => i.id !== deleting.id));
      setDeleting(null);
    } catch (e) {
      setError(getAuthErrorMessage(e));
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="mx-auto max-w-6xl animate-fade-in">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-100 text-teal-700">
            <FileText className="h-6 w-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-teal-700">Finance</p>
            <h1 className="mt-1 text-2xl font-semibold">Invoices</h1>
          </div>
        </div>
        <div className="flex flex-wrap gap-2"><Link href="/invoices/new"><Button className="gap-2 rounded-xl bg-indigo-600 hover:bg-indigo-700"><Sparkles className="h-4 w-4" />Upload with AI</Button></Link><Link href="/invoices/new"><Button variant="secondary" className="gap-2 rounded-xl"><Plus className="h-4 w-4" />Manual entry</Button></Link></div>
      </div>
      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <InputSearch value={query} onChange={setQuery} />
        <select aria-label="Filter invoice status" className="min-h-10 rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-700 focus:border-teal-500 focus:outline-none" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as "ALL" | InvoiceStatus)}><option value="ALL">All statuses</option><option value="PENDING">Pending</option><option value="PAID">Paid</option><option value="OVERDUE">Overdue</option></select>
        <select aria-label="Filter invoice direction" className="min-h-10 rounded-xl border border-slate-300 bg-white px-3 text-sm text-slate-700 focus:border-teal-500 focus:outline-none" value={directionFilter} onChange={(e) => setDirectionFilter(e.target.value as "ALL" | InvoiceDirection)}><option value="ALL">All directions</option><option value="INCOME">Income</option><option value="EXPENSE">Expense</option></select>
      </div>
      {error && (
        <div className="mt-4">
          <ErrorState title="Could not load invoices" description={error} />
        </div>
      )}
      {loading ? (
        <div className="mt-6 space-y-3">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : visible.length === 0 ? (
        <div className="mt-6">
          <EmptyState 
            title={invoices.length ? "No matching invoices" : "No invoices yet"} 
            description={invoices.length ? "Try a different search." : "Create your first invoice to get started."} 
          />
        </div>
      ) : (
        <Card className="mt-6 overflow-x-auto p-0">
          <table className="hidden w-full text-left text-sm md:table">
            <thead className="border-b bg-slate-50 text-slate-600">
              <tr>
                {["Invoice", "Vendor", "Date", "Total", "Status", "Direction", "Action"].map(h => (
                  <th className="px-4 py-3 font-semibold" key={h}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y text-slate-700">
              {visible.map(i => <Row key={i.id} invoice={i} onDelete={setDeleting} />)}
            </tbody>
          </table>
          <div className="divide-y md:hidden">
            {visible.map(i => <MobileRow key={i.id} invoice={i} onDelete={setDeleting} />)}
          </div>
        </Card>
      )}
      <Dialog open={Boolean(deleting)} title="Delete invoice" onClose={() => !pending && setDeleting(null)}>
        <p className="text-sm text-slate-600">Delete <strong>{deleting?.invoiceNumber}</strong>? This cannot be undone.</p>
        <div className="mt-5 flex justify-end gap-3">
          <Button variant="secondary" onClick={() => setDeleting(null)} disabled={pending}>Cancel</Button>
          <Button variant="danger" onClick={remove} disabled={pending}>{pending ? "Deleting…" : "Delete invoice"}</Button>
        </div>
      </Dialog>
    </section>
  );
}


function InputSearch({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <div className="relative max-w-md w-full">
      <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
        <Search className="h-4 w-4 text-slate-400" />
      </div>
      <input 
        aria-label="Search invoices" 
        className="min-h-10 w-full rounded-lg border border-slate-300 bg-white pl-9 pr-3 text-sm focus:border-teal-500 focus:outline-none focus:ring-1 focus:ring-teal-500" 
        placeholder="Search invoice number or vendor" 
        value={value} 
        onChange={e => onChange(e.target.value)} 
      />
    </div>
  );
}

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

function Row({ invoice: i, onDelete }: { invoice: Invoice; onDelete: (i: Invoice) => void }) {
  return (
    <tr className="transition-colors hover:bg-slate-50">
      <td className="px-4 py-4"><Link className="font-medium text-teal-700 hover:underline" href={`/invoices/${i.id}`}>{i.invoiceNumber}</Link></td>
      <td className="px-4 py-4 font-medium">{i.vendorName}</td>
      <td className="px-4 py-4 text-slate-500">{i.invoiceDate}</td>
      <td className="px-4 py-4 font-medium">{i.currency} {i.totalAmount.toFixed(2)}</td>
      <td className="px-4 py-4"><StatusBadge status={i.status} /></td>
      <td className="px-4 py-4"><DirectionBadge direction={i.direction} /></td>
      <td className="px-4 py-4">
        <div className="flex items-center gap-2">
          <Link href={`/invoices/${i.id}/edit`} aria-label="Edit invoice" className="rounded p-2 text-slate-400 hover:bg-slate-100 hover:text-teal-700">
            <Pencil className="h-4 w-4" />
          </Link>
          <button aria-label="Delete invoice" className="rounded p-2 text-slate-400 hover:bg-red-50 hover:text-red-700" onClick={() => onDelete(i)} type="button">
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </td>
    </tr>
  );
}

function MobileRow({ invoice: i, onDelete }: { invoice: Invoice; onDelete: (i: Invoice) => void }) {
  return (
    <div className="p-4 transition-colors hover:bg-slate-50">
      <div className="flex items-center justify-between">
        <Link className="font-medium text-teal-700 hover:underline" href={`/invoices/${i.id}`}>{i.invoiceNumber}</Link>
        <StatusBadge status={i.status} />
      </div>
      <p className="mt-1 text-sm font-medium text-slate-800">{i.vendorName}</p>
      <div className="mt-2 flex items-center justify-between text-sm text-slate-500">
        <span>{i.invoiceDate} · {i.currency} {i.totalAmount.toFixed(2)}</span>
        <DirectionBadge direction={i.direction} />
      </div>
      <div className="mt-4 flex items-center gap-3">
        <Link className="flex items-center gap-1 text-sm font-medium text-teal-700 hover:underline" href={`/invoices/${i.id}/edit`}>
          <Pencil className="h-4 w-4" /> Edit
        </Link>
        <button className="flex items-center gap-1 text-sm font-medium text-red-700 hover:underline" onClick={() => onDelete(i)} type="button">
          <Trash2 className="h-4 w-4" /> Delete
        </button>
      </div>
    </div>
  );
}
