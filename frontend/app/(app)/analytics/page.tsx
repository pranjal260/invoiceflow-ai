"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { TrendingUp, FolderOpen, CircleDollarSign } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { CurrencySelector } from "@/components/analytics/currency-selector";
import { formatCurrency, formatMonth } from "@/lib/format";
import { analyticsService } from "@/services/analytics-service";
import type {
  CategoryAnalyticsEntry,
  MonthlyAnalyticsEntry,
  RevenueAnalyticsEntry,
} from "@/types/api";

export default function AnalyticsPage() {
  const [monthly, setMonthly] = useState<MonthlyAnalyticsEntry[]>([]);
  const [categories, setCategories] = useState<CategoryAnalyticsEntry[]>([]);
  const [revenue, setRevenue] = useState<RevenueAnalyticsEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCurrency, setSelectedCurrency] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [m, c, r] = await Promise.all([
        analyticsService.monthly(),
        analyticsService.categories(),
        analyticsService.revenue(),
      ]);
      setMonthly(m.months);
      setCategories(c.categories);
      setRevenue(r.trends);
    } catch {
      setError("Unable to load analytics. Please try again.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  // Derive all currencies from all endpoints
  const currencies = useMemo(() => {
    const set = new Set<string>();
    monthly.forEach((e) => set.add(e.currency));
    categories.forEach((e) => set.add(e.currency));
    revenue.forEach((e) => set.add(e.currency));
    return Array.from(set).sort();
  }, [monthly, categories, revenue]);

  // Default currency selection
  const activeCurrency = currencies.includes(selectedCurrency)
    ? selectedCurrency
    : currencies[0] ?? "";

  // Set default on first load
  useEffect(() => {
    if (currencies.length > 0 && !currencies.includes(selectedCurrency)) {
      setSelectedCurrency(currencies[0]);
    }
  }, [currencies, selectedCurrency]);

  // Filter data by selected currency
  const filteredMonthly = useMemo(
    () => monthly.filter((e) => e.currency === activeCurrency),
    [monthly, activeCurrency],
  );
  const filteredCategories = useMemo(
    () => categories.filter((e) => e.currency === activeCurrency),
    [categories, activeCurrency],
  );
  const filteredRevenue = useMemo(
    () => revenue.filter((e) => e.currency === activeCurrency),
    [revenue, activeCurrency],
  );

  const isEmpty = monthly.length === 0 && categories.length === 0 && revenue.length === 0;

  return (
    <section className="mx-auto max-w-6xl animate-fade-in">
      <div>
        <p className="text-sm font-medium text-teal-700">InvoiceFlow AI</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight sm:text-3xl">Analytics</h1>
        <p className="mt-1 text-sm text-slate-500">
          Financial trends, category breakdowns, and revenue analysis.
        </p>
      </div>

      {loading && (
        <div className="mt-6 space-y-4">
          <Skeleton className="h-10 w-48 rounded-full" />
          <Skeleton className="h-64 w-full rounded-xl" />
          <Skeleton className="h-64 w-full rounded-xl" />
        </div>
      )}

      {error && (
        <div className="mt-6">
          <ErrorState title="Analytics unavailable" description={error} />
          <Button className="mt-3" onClick={() => void load()} variant="secondary">
            Retry
          </Button>
        </div>
      )}

      {!loading && !error && isEmpty && (
        <div className="mt-6">
          <EmptyState
            title="No analytics data"
            description="Create invoices to see financial analytics here."
          />
        </div>
      )}

      {!loading && !error && !isEmpty && (
        <>
          {/* Currency selector */}
          <div className="mt-6">
            <CurrencySelector
              currencies={currencies}
              selected={activeCurrency}
              onSelect={setSelectedCurrency}
            />
          </div>

          {/* Monthly Income vs Expenses */}
          <Card className="mt-6">
            <div className="flex items-center gap-2">
              <div className="rounded-full bg-teal-100 p-2 text-teal-600">
                <TrendingUp className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-lg font-semibold">Monthly overview</h2>
                <p className="text-sm text-slate-500">
                  Income vs expenses by month ({activeCurrency}).
                </p>
              </div>
            </div>
            {filteredMonthly.length === 0 ? (
              <div className="mt-4">
                <EmptyState title="No monthly data" description={`No invoice data in ${activeCurrency}.`} />
              </div>
            ) : (
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="border-b bg-slate-50 text-slate-500">
                    <tr>
                      <th className="px-4 py-3 font-medium">Month</th>
                      <th className="px-4 py-3 font-medium text-right">Income</th>
                      <th className="px-4 py-3 font-medium text-right">Expenses</th>
                      <th className="px-4 py-3 font-medium text-right">Net</th>
                      <th className="px-4 py-3 font-medium text-right">Invoices</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {filteredMonthly.map((entry) => {
                      const net = entry.revenue - entry.expenses;
                      return (
                        <tr key={entry.month} className="transition-colors hover:bg-slate-50/50">
                          <td className="px-4 py-3 font-medium">{formatMonth(entry.month)}</td>
                          <td className="px-4 py-3 text-right text-teal-700">
                            {formatCurrency(entry.revenue, activeCurrency)}
                          </td>
                          <td className="px-4 py-3 text-right text-red-700">
                            {formatCurrency(entry.expenses, activeCurrency)}
                          </td>
                          <td className={`px-4 py-3 text-right font-medium ${net >= 0 ? "text-teal-700" : "text-red-700"}`}>
                            {formatCurrency(net, activeCurrency)}
                          </td>
                          <td className="px-4 py-3 text-right text-slate-600">{entry.invoiceCount}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                  {filteredMonthly.length > 1 && (
                    <tfoot className="border-t bg-slate-50 font-medium">
                      <tr>
                        <td className="px-4 py-3">Total</td>
                        <td className="px-4 py-3 text-right text-teal-700">
                          {formatCurrency(filteredMonthly.reduce((s, e) => s + e.revenue, 0), activeCurrency)}
                        </td>
                        <td className="px-4 py-3 text-right text-red-700">
                          {formatCurrency(filteredMonthly.reduce((s, e) => s + e.expenses, 0), activeCurrency)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          {formatCurrency(
                            filteredMonthly.reduce((s, e) => s + e.revenue - e.expenses, 0),
                            activeCurrency,
                          )}
                        </td>
                        <td className="px-4 py-3 text-right text-slate-600">
                          {filteredMonthly.reduce((s, e) => s + e.invoiceCount, 0)}
                        </td>
                      </tr>
                    </tfoot>
                  )}
                </table>

                {/* Visual bars */}
                <div className="mt-8 space-y-4 border-t pt-6">
                  {filteredMonthly.map((entry) => {
                    const max = Math.max(
                      ...filteredMonthly.map((e) => Math.max(e.revenue, e.expenses)),
                      1,
                    );
                    return (
                      <div key={entry.month + "-bar"}>
                        <p className="mb-1.5 text-xs font-medium text-slate-500">{formatMonth(entry.month)}</p>
                        <div className="flex flex-col gap-1.5">
                          <div
                            className="h-3 rounded-md bg-teal-500 transition-all duration-500"
                            style={{ width: `${(entry.revenue / max) * 100}%`, minWidth: entry.revenue > 0 ? "4px" : "0" }}
                            title={`Income: ${formatCurrency(entry.revenue, activeCurrency)}`}
                          />
                          <div
                            className="h-3 rounded-md bg-red-400 transition-all duration-500"
                            style={{ width: `${(entry.expenses / max) * 100}%`, minWidth: entry.expenses > 0 ? "4px" : "0" }}
                            title={`Expenses: ${formatCurrency(entry.expenses, activeCurrency)}`}
                          />
                        </div>
                      </div>
                    );
                  })}
                  <div className="flex gap-4 pt-2 text-xs font-medium text-slate-500">
                    <span className="flex items-center gap-1.5">
                      <span className="inline-block h-2.5 w-2.5 rounded-sm bg-teal-500" /> Income
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="inline-block h-2.5 w-2.5 rounded-sm bg-red-400" /> Expenses
                    </span>
                  </div>
                </div>
              </div>
            )}
          </Card>

          {/* Category breakdown */}
          <Card className="mt-6">
            <div className="flex items-center gap-2">
              <div className="rounded-full bg-slate-100 p-2 text-slate-600">
                <FolderOpen className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-lg font-semibold">Category breakdown</h2>
                <p className="text-sm text-slate-500">
                  Spending and income by category ({activeCurrency}).
                </p>
              </div>
            </div>
            {filteredCategories.length === 0 ? (
              <div className="mt-4">
                <EmptyState title="No category data" description={`No categorized invoices in ${activeCurrency}.`} />
              </div>
            ) : (
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="border-b bg-slate-50 text-slate-500">
                    <tr>
                      <th className="px-4 py-3 font-medium">Category</th>
                      <th className="px-4 py-3 font-medium">Direction</th>
                      <th className="px-4 py-3 font-medium text-right">Total</th>
                      <th className="px-4 py-3 font-medium text-right">Invoices</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {filteredCategories.map((entry, index) => (
                      <tr key={`${entry.categoryName}-${entry.direction}-${index}`} className="transition-colors hover:bg-slate-50/50">
                        <td className="px-4 py-3 font-medium">{entry.categoryName}</td>
                        <td className="px-4 py-3">
                          <span
                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                              entry.direction === "INCOME"
                                ? "bg-teal-50 text-teal-700"
                                : "bg-red-50 text-red-700"
                            }`}
                          >
                            {entry.direction}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right font-medium">
                          {formatCurrency(entry.totalAmount, activeCurrency)}
                        </td>
                        <td className="px-4 py-3 text-right text-slate-600">{entry.invoiceCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          {/* Revenue trends */}
          <Card className="mt-6">
            <div className="flex items-center gap-2">
              <div className="rounded-full bg-teal-100 p-2 text-teal-600">
                <CircleDollarSign className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-lg font-semibold">Revenue trends</h2>
                <p className="text-sm text-slate-500">
                  Monthly income trend ({activeCurrency}).
                </p>
              </div>
            </div>
            {filteredRevenue.length === 0 ? (
              <div className="mt-4">
                <EmptyState title="No revenue data" description={`No INCOME invoices in ${activeCurrency}.`} />
              </div>
            ) : (
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="border-b bg-slate-50 text-slate-500">
                    <tr>
                      <th className="px-4 py-3 font-medium">Month</th>
                      <th className="px-4 py-3 font-medium text-right">Revenue</th>
                      <th className="px-4 py-3 font-medium text-right">Invoices</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {filteredRevenue.map((entry) => (
                      <tr key={entry.month} className="transition-colors hover:bg-slate-50/50">
                        <td className="px-4 py-3 font-medium">{formatMonth(entry.month)}</td>
                        <td className="px-4 py-3 text-right text-teal-700 font-medium">
                          {formatCurrency(entry.revenue, activeCurrency)}
                        </td>
                        <td className="px-4 py-3 text-right text-slate-600">{entry.invoiceCount}</td>
                      </tr>
                    ))}
                  </tbody>
                  {filteredRevenue.length > 1 && (
                    <tfoot className="border-t bg-slate-50 font-medium">
                      <tr>
                        <td className="px-4 py-3">Total</td>
                        <td className="px-4 py-3 text-right text-teal-700">
                          {formatCurrency(filteredRevenue.reduce((s, e) => s + e.revenue, 0), activeCurrency)}
                        </td>
                        <td className="px-4 py-3 text-right text-slate-600">
                          {filteredRevenue.reduce((s, e) => s + e.invoiceCount, 0)}
                        </td>
                      </tr>
                    </tfoot>
                  )}
                </table>

                {/* Visual bars */}
                <div className="mt-8 space-y-4 border-t pt-6">
                  {filteredRevenue.map((entry) => {
                    const max = Math.max(...filteredRevenue.map((e) => e.revenue), 1);
                    return (
                      <div key={entry.month + "-rev-bar"}>
                        <p className="mb-1.5 text-xs font-medium text-slate-500">{formatMonth(entry.month)}</p>
                        <div
                          className="h-3 rounded-md bg-teal-500 transition-all duration-500"
                          style={{ width: `${(entry.revenue / max) * 100}%`, minWidth: entry.revenue > 0 ? "4px" : "0" }}
                          title={formatCurrency(entry.revenue, activeCurrency)}
                        />
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </Card>
        </>
      )}
    </section>
  );
}
