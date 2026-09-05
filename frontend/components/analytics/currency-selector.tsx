"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";

type Props = {
  currencies: string[];
  selected: string;
  onSelect: (currency: string) => void;
};

export function CurrencySelector({ currencies, selected, onSelect }: Props) {
  if (currencies.length <= 1) return null;

  return (
    <div className="flex flex-wrap gap-2" role="tablist" aria-label="Select currency">
      {currencies.map((currency) => (
        <button
          key={currency}
          role="tab"
          aria-selected={currency === selected}
          className={cn(
            "rounded-full border px-4 py-1.5 text-sm font-medium transition-all",
            currency === selected
              ? "border-teal-700 bg-teal-700 text-white shadow-sm"
              : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50 hover:text-slate-900",
          )}
          onClick={() => onSelect(currency)}
          type="button"
        >
          {currency}
        </button>
      ))}
    </div>
  );
}

/** Hook to manage currency selection from a list of available currencies. */
export function useCurrencySelector(currencies: string[]) {
  const [selected, setSelected] = useState(currencies[0] ?? "");

  // If selected currency is no longer in the list, reset
  const effective = currencies.includes(selected) ? selected : currencies[0] ?? "";

  return { selected: effective, setSelected, currencies };
}
