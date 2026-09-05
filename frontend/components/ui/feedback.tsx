import { CheckCircle2 } from "lucide-react";

export function SuccessFeedback({ message, onDismiss }: { message: string; onDismiss: () => void }) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 animate-fade-in">
      <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
      <p className="flex-1">{message}</p>
      <button
        className="ml-2 rounded p-1 text-emerald-600 transition-colors hover:bg-emerald-100 hover:text-emerald-800"
        onClick={onDismiss}
        type="button"
        aria-label="Dismiss"
      >
        ✕
      </button>
    </div>
  );
}
