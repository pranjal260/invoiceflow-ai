import { Card } from "@/components/ui/card";

export function PagePlaceholder({ title, description }: { title: string; description: string }) {
  return (
    <section className="mx-auto max-w-6xl">
      <p className="text-sm font-medium text-teal-700">InvoiceFlow AI</p>
      <h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl">{title}</h1>
      <Card className="mt-6 max-w-2xl"><p className="text-slate-600">{description}</p></Card>
    </section>
  );
}
