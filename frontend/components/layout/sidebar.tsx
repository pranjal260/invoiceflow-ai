"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { cn } from "@/lib/utils";
import { 
  LayoutDashboard, 
  FileText, 
  FolderOpen, 
  BarChart3, 
  Settings, 
  Sparkles, 
  LogOut 
} from "lucide-react";
import { Button } from "@/components/ui/button";

const mainNavigation = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Invoices", href: "/invoices", icon: FileText },
  { label: "Categories", href: "/categories", icon: FolderOpen },
  { label: "Analytics", href: "/analytics", icon: BarChart3 },
];

const accountNavigation = [
  { label: "Settings", href: "/settings/profile", icon: Settings },
];

export function Sidebar({ onClose }: { onClose?: () => void }) {
  const pathname = usePathname();
  const { logout, user } = useAuth();
  
  const firstName = user?.firstName || "User";
  const initials = (user?.firstName?.[0] || "") + (user?.lastName?.[0] || "");
  const displayName = user ? `${user.firstName} ${user.lastName}`.trim() : "Loading user";

  return (
    <aside className="flex h-full w-full flex-col bg-white">
      {/* Header */}
      <div className="flex h-16 items-center gap-2 px-6 border-b border-slate-100">
        <Sparkles className="h-5 w-5 text-teal-600" />
        <Link 
          href="/dashboard" 
          className="font-semibold tracking-tight text-slate-900"
          onClick={onClose}
        >
          InvoiceFlow AI
        </Link>
      </div>

      {/* Navigation */}
      <nav aria-label="Main navigation" className="flex-1 overflow-y-auto py-4">
        <div className="px-3">
          <p className="mb-2 px-4 text-xs font-semibold tracking-wider text-slate-400 uppercase">Main</p>
          <div className="space-y-1">
            {mainNavigation.map(({ label, href, icon: Icon }) => {
              const isActive = pathname === href || (href !== "/dashboard" && pathname.startsWith(`${href}/`));
              return (
                <Link
                  key={href}
                  href={href}
                  onClick={onClose}
                  className={cn(
                    "flex items-center gap-3 rounded-lg px-4 py-2 text-sm transition-colors relative",
                    isActive 
                      ? "bg-teal-50 font-medium text-teal-700" 
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                  )}
                >
                  {isActive && (
                    <div className="absolute left-0 top-0 bottom-0 w-[3px] rounded-r bg-teal-700" />
                  )}
                  <Icon className="h-4 w-4" />
                  {label}
                </Link>
              );
            })}
          </div>
        </div>

        <div className="mt-8 px-3">
          <p className="mb-2 px-4 text-xs font-semibold tracking-wider text-slate-400 uppercase">Account</p>
          <div className="space-y-1">
            {accountNavigation.map(({ label, href, icon: Icon }) => {
              const isActive = pathname === href || pathname.startsWith(`${href}/`);
              return (
                <Link
                  key={href}
                  href={href}
                  onClick={onClose}
                  className={cn(
                    "flex items-center gap-3 rounded-lg px-4 py-2 text-sm transition-colors relative",
                    isActive 
                      ? "bg-teal-50 font-medium text-teal-700" 
                      : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                  )}
                >
                  {isActive && (
                    <div className="absolute left-0 top-0 bottom-0 w-[3px] rounded-r bg-teal-700" />
                  )}
                  <Icon className="h-4 w-4" />
                  {label}
                </Link>
              );
            })}
          </div>
        </div>
      </nav>

      {/* User Footer */}
      <div className="border-t border-slate-100 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-teal-100 text-teal-700 font-medium text-xs">
              {initials || "U"}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-slate-900">{firstName}</p>
              <p className="truncate text-xs text-slate-500">{user?.email ?? ""}</p>
            </div>
          </div>
          <Button 
            variant="ghost" 
            className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100" 
            onClick={logout}
            title="Log out"
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </aside>
  );
}
