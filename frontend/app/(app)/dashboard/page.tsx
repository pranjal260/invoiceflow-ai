"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { FileText, CheckCircle2, Clock, AlertCircle, TrendingUp, TrendingDown, CircleDollarSign, Plus, FolderOpen, BarChart3 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { MetricCard } from "@/components/dashboard/metric-card";
import { useAuth } from "@/context/AuthContext";
import { formatCurrency } from "@/lib/format";
import { dashboardService } from "@/services/dashboard-service";
import type { DashboardCurrencySummary } from "@/types/api";

export default function DashboardPage() {
  const { user } = useAuth();
  const [summaries, setSummaries] = useState<DashboardCurrencySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await dashboardService.getSummary();
      setSummaries(response.summaries);
    } catch {
      setError("Unable to load dashboard. Please try again.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const totalInvoices = summaries.reduce((sum, s) => sum + s.invoiceCount, 0);
  const totalPaid = summaries.reduce((sum, s) => sum + s.paidInvoices, 0);
  const totalPending = summaries.reduce((sum, s) => sum + s.pendingInvoices, 0);
  const totalOverdue = totalInvoices - totalPaid - totalPending;

  const currentDate = new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  return (
    <section className="mx-auto max-w-6xl animate-fade-in">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-teal-700">{currentDate}</p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight sm:text-3xl">
            {user ? `Welcome, ${user.firstName}` : "Dashboard"}
          </h1>
          <p className="mt-1 text-sm text-slate-500">Overview of your invoices and finances.</p>
        </div>
        <Link href="/invoices/new">
          <Button className="gap-2"><Plus className="h-4 w-4" /> Create invoice</Button>
        </Link>
      </div>

      {loading && (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Skeleton className="h-32 w-full rounded-xl" />
          <Skeleton className="h-32 w-full rounded-xl" />
          <Skeleton className="h-32 w-full rounded-xl" />
          <Skeleton className="h-32 w-full rounded-xl" />
        </div>
      )}

      {error && (
        <div className="mt-6">
          <ErrorState title="Dashboard unavailable" description={error} />
          <Button className="mt-3" onClick={() => void load()} variant="secondary">
            Retry
          </Button>
        </div>
      )}

      {!loading && !error && summaries.length === 0 && (
        <div className="mt-6">
          <EmptyState
            title="No invoices yet"
            description="Create your first invoice to see financial summaries here."
          />
          <div className="mt-6 flex justify-center">
            <Link href="/invoices/new">
              <Button className="gap-2"><Plus className="h-4 w-4" /> Create your first invoice</Button>
            </Link>
          </div>
        </div>
      )}

      {!loading && !error && summaries.length > 0 && (
        <>
          {/* Global status overview */}
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <MetricCard
              label="Total invoices"
              value={String(totalInvoices)}
              accent="slate"
              icon={<FileText className="h-4 w-4" />}
            />
            <MetricCard
              label="Paid"
              value={String(totalPaid)}
              accent="teal"
              icon={<CheckCircle2 className="h-4 w-4" />}
            />
            <MetricCard
              label="Pending"
              value={String(totalPending)}
              accent="amber"
              icon={<Clock className="h-4 w-4" />}
            />
            <MetricCard
              label="Overdue"
              value={String(totalOverdue)}
              accent={totalOverdue > 0 ? "red" : "slate"}
              icon={<AlertCircle className="h-4 w-4" />}
            />
          </div>

          {/* Per-currency financial summaries */}
          {summaries.map((summary) => (
            <Card key={summary.currency} className="mt-6">
              <h2 className="text-lg font-semibold">{summary.currency}</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-3">
                <MetricCard
                  label="Income"
                  value={formatCurrency(summary.revenue, summary.currency)}
                  detail={`from INCOME invoices`}
                  accent="teal"
                  icon={<TrendingUp className="h-4 w-4" />}
                />
                <MetricCard
                  label="Expenses"
                  value={formatCurrency(summary.expenses, summary.currency)}
                  detail={`from EXPENSE invoices`}
                  accent="red"
                  icon={<TrendingDown className="h-4 w-4" />}
                />
                <MetricCard
                  label="Net"
                  value={formatCurrency(summary.revenue - summary.expenses, summary.currency)}
                  detail={`${summary.invoiceCount} invoices`}
                  accent="slate"
                  icon={<CircleDollarSign className="h-4 w-4" />}
                />
              </div>
              <div className="mt-4 flex flex-wrap gap-4 text-sm font-medium text-slate-600">
                <span className="flex items-center gap-1.5"><CheckCircle2 className="h-4 w-4 text-teal-600" /> {summary.paidInvoices} paid</span>
                <span className="flex items-center gap-1.5"><Clock className="h-4 w-4 text-amber-600" /> {summary.pendingInvoices} pending</span>
                <span className="flex items-center gap-1.5"><AlertCircle className="h-4 w-4 text-red-600" /> {summary.invoiceCount - summary.paidInvoices - summary.pendingInvoices} overdue</span>
              </div>
            </Card>
          ))}

          {/* Quick actions */}
          <div className="mt-6">
            <h2 className="text-lg font-semibold">Quick actions</h2>
            <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <Link href="/invoices" className="flex items-center gap-3 rounded-xl border bg-white p-4 text-sm font-medium text-slate-700 shadow-sm transition-all hover:border-slate-300 hover:bg-slate-50">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-600"><FileText className="h-5 w-5" /></div>
                View invoices
              </Link>
              <Link href="/invoices/new" className="flex items-center gap-3 rounded-xl border bg-white p-4 text-sm font-medium text-slate-700 shadow-sm transition-all hover:border-slate-300 hover:bg-slate-50">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-teal-100 text-teal-600"><Plus className="h-5 w-5" /></div>
                Create invoice
              </Link>
              <Link href="/categories" className="flex items-center gap-3 rounded-xl border bg-white p-4 text-sm font-medium text-slate-700 shadow-sm transition-all hover:border-slate-300 hover:bg-slate-50">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-600"><FolderOpen className="h-5 w-5" /></div>
                Categories
              </Link>
              <Link href="/analytics" className="flex items-center gap-3 rounded-xl border bg-white p-4 text-sm font-medium text-slate-700 shadow-sm transition-all hover:border-slate-300 hover:bg-slate-50">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-600"><BarChart3 className="h-5 w-5" /></div>
                Analytics
              </Link>
            </div>
          </div>
        </>
      )}
    </section>
  );
}
