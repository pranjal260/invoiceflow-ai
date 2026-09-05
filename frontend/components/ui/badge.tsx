import type { ReactNode } from "react";

type BadgeVariant = "teal" | "green" | "amber" | "red" | "slate" | "blue";

const variantStyles: Record<BadgeVariant, string> = {
  teal: "bg-teal-50 text-teal-700 border-teal-200",
  green: "bg-emerald-50 text-emerald-700 border-emerald-200",
  amber: "bg-amber-50 text-amber-700 border-amber-200",
  red: "bg-red-50 text-red-700 border-red-200",
  slate: "bg-slate-50 text-slate-600 border-slate-200",
  blue: "bg-blue-50 text-blue-700 border-blue-200",
};

export function Badge({
  variant = "slate",
  children,
  className = "",
}: {
  variant?: BadgeVariant;
  children: ReactNode;
  className?: string;
}) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium ${variantStyles[variant]} ${className}`}
    >
      {children}
    </span>
  );
}
