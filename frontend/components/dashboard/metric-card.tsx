import { cn } from "@/lib/utils";
import { ReactNode } from "react";

type Props = {
  label: string;
  value: string;
  detail?: string;
  accent?: "teal" | "red" | "amber" | "slate";
  icon?: ReactNode;
};

const accents = {
  teal: "border-teal-200 bg-teal-50",
  red: "border-red-200 bg-red-50",
  amber: "border-amber-200 bg-amber-50",
  slate: "border-slate-200 bg-white",
};

const iconBackgrounds = {
  teal: "bg-teal-100 text-teal-600",
  red: "bg-red-100 text-red-600",
  amber: "bg-amber-100 text-amber-600",
  slate: "bg-slate-100 text-slate-600",
};

export function MetricCard({ label, value, detail, accent = "slate", icon }: Props) {
  return (
    <div className={cn("rounded-xl border p-4 shadow-sm transition-shadow hover:shadow-md", accents[accent])}>
      <div className="flex items-start justify-between">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        {icon && (
          <div className={cn("flex h-8 w-8 items-center justify-center rounded-full", iconBackgrounds[accent])}>
            {icon}
          </div>
        )}
      </div>
      <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
      {detail && <p className="mt-1 text-xs text-slate-500">{detail}</p>}
    </div>
  );
}
