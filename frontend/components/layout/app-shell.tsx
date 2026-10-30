"use client";

import { useState } from "react";
import type { ReactNode } from "react";
import { Sidebar } from "@/components/layout/sidebar";
import { Menu, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { Sparkles } from "lucide-react";

export function AppShell({ children }: { children: ReactNode }) {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden bg-[#f7faf9] flex-col md:flex-row">
      {/* Mobile Top Bar */}
      <div className="md:hidden flex h-16 items-center justify-between bg-white border-b border-slate-200 px-4 flex-shrink-0">
        <Link href="/dashboard" className="flex items-center gap-2 font-semibold tracking-tight text-slate-900"><Sparkles className="h-4 w-4 text-teal-600" />InvoiceFlow AI</Link>
        <Button 
          variant="ghost" 
          className="p-2"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
        >
          {isMobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </Button>
      </div>

      {/* Sidebar - fixed on desktop, toggleable on mobile */}
      <div className={`
        fixed inset-y-0 left-0 z-50 w-64 bg-white border-r border-slate-200 transform transition-transform duration-200 ease-in-out 
        md:relative md:translate-x-0
        ${isMobileMenuOpen ? "translate-x-0" : "-translate-x-full"}
      `}>
        <Sidebar onClose={() => setIsMobileMenuOpen(false)} />
      </div>

      {/* Mobile Overlay */}
      {isMobileMenuOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/50 z-40 md:hidden" 
          onClick={() => setIsMobileMenuOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Main Content */}
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <header className="hidden h-16 shrink-0 items-center justify-between border-b border-slate-200/80 bg-white/85 px-6 backdrop-blur md:flex lg:px-8">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">Workspace</p>
            <p className="mt-0.5 text-sm font-medium text-slate-700">Your financial command center</p>
          </div>
          <Link href="/invoices/new"><Button className="gap-2 rounded-xl bg-indigo-600 shadow-sm hover:bg-indigo-700"><Sparkles className="h-4 w-4" />Upload with AI</Button></Link>
        </header>
        <main className="min-w-0 flex-1 overflow-y-auto">
        <div className="p-5 sm:p-8 max-w-full">
          {children}
        </div>
        </main>
      </div>
    </div>
  );
}
