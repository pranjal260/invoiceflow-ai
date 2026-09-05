import type { InputHTMLAttributes } from "react";

export function Input({ className = "", ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={`min-h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-900 placeholder:text-slate-400 transition-colors duration-150 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 focus:outline-none disabled:bg-slate-50 disabled:text-slate-500 ${className}`}
      {...props}
    />
  );
}
