import type { ReactNode } from "react";
import Link from "next/link";
import { Sparkles, FileText, BarChart3, Shield } from "lucide-react";
import { GuestRoute } from "@/components/auth/auth-route-guards";

export default function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <GuestRoute>
      <main className="flex min-h-screen bg-white">
        {/* LEFT PANEL */}
        <div className="hidden lg:flex lg:w-1/2 flex-col justify-between bg-slate-900 text-white p-12 xl:p-16">
          <div>
            <Link className="inline-flex items-center gap-2 text-2xl font-bold text-teal-400 hover:text-teal-300 transition-colors" href="/">
              <Sparkles className="h-8 w-8" />
              InvoiceFlow AI
            </Link>
            <p className="mt-8 text-3xl font-medium leading-snug text-slate-200 max-w-md">
              AI-powered invoice management for modern businesses
            </p>
          </div>
          <div className="space-y-8 mb-8">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-teal-500/20 text-teal-400">
                <FileText className="h-6 w-6" />
              </div>
              <p className="text-lg font-medium text-slate-300">Smart invoice tracking</p>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-teal-500/20 text-teal-400">
                <Sparkles className="h-6 w-6" />
              </div>
              <p className="text-lg font-medium text-slate-300">AI-powered data extraction</p>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-teal-500/20 text-teal-400">
                <BarChart3 className="h-6 w-6" />
              </div>
              <p className="text-lg font-medium text-slate-300">Financial analytics & insights</p>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-teal-500/20 text-teal-400">
                <Shield className="h-6 w-6" />
              </div>
              <p className="text-lg font-medium text-slate-300">Secure document storage</p>
            </div>
          </div>
        </div>
        {/* RIGHT PANEL */}
        <div className="flex w-full lg:w-1/2 items-center justify-center p-6 sm:p-12 overflow-y-auto">
          <div className="w-full max-w-md">
            <div className="lg:hidden mb-8 text-center sm:text-left">
              <Link className="inline-flex items-center gap-2 text-2xl font-bold text-teal-600" href="/">
                <Sparkles className="h-8 w-8" />
                InvoiceFlow AI
              </Link>
            </div>
            {children}
          </div>
        </div>
      </main>
    </GuestRoute>
  );
}
