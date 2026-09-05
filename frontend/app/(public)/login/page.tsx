"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/context/AuthContext";
import { getAuthErrorMessage } from "@/lib/auth-errors";
import { LogIn } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!email.trim() || !password) {
      setError("Enter your email and password.");
      return;
    }

    setIsSubmitting(true);
    try {
      await login({ email: email.trim(), password });
      router.replace(searchParams.get("next") || "/dashboard");
    } catch (requestError) {
      setError(getAuthErrorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      <div className="mb-8 text-center sm:text-left">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Welcome back</h1>
        <p className="mt-2 text-sm text-slate-600">Manage invoices and financial insights securely.</p>
      </div>
      <form className="space-y-5" onSubmit={handleSubmit} noValidate>
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input autoComplete="email" id="email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="password">Password</Label>
          <Input autoComplete="current-password" id="password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
        </div>
        {error && <p aria-live="polite" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-800">{error}</p>}
        <Button className="w-full gap-2 mt-2" disabled={isSubmitting} type="submit">
          <LogIn className="h-4 w-4" />
          {isSubmitting ? "Logging in…" : "Log in"}
        </Button>
      </form>
      
      <div className="mt-8 flex items-center before:mt-0.5 before:flex-1 before:border-t before:border-slate-200 after:mt-0.5 after:flex-1 after:border-t after:border-slate-200">
        <span className="mx-4 mb-0 text-center text-sm font-medium text-slate-500">New to InvoiceFlow AI?</span>
      </div>
      
      <div className="mt-6 text-center">
        <Link className="text-sm font-semibold text-teal-600 hover:text-teal-700 hover:underline" href="/register">Create an account</Link>
      </div>
    </>
  );
}
