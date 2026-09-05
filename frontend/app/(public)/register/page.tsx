"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/context/AuthContext";
import { getAuthErrorMessage } from "@/lib/auth-errors";
import { UserPlus } from "lucide-react";

type RegistrationForm = {
  firstName: string; lastName: string; companyName: string; email: string; password: string;
  confirmPassword: string; phoneNumber: string; country: string;
};

const initialForm: RegistrationForm = {
  firstName: "", lastName: "", companyName: "", email: "", password: "", confirmPassword: "", phoneNumber: "", country: "",
};

export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateField(field: keyof RegistrationForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!form.firstName.trim() || !form.lastName.trim() || !form.email.trim() || !form.password) {
      setError("First name, last name, email, and password are required.");
      return;
    }
    if (form.password.length < 8) {
      setError("Password must contain at least 8 characters.");
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError("Password confirmation does not match.");
      return;
    }

    setIsSubmitting(true);
    try {
      await register({
        firstName: form.firstName.trim(), lastName: form.lastName.trim(), companyName: form.companyName.trim() || undefined,
        email: form.email.trim(), password: form.password, phoneNumber: form.phoneNumber.trim() || undefined,
        country: form.country.trim() || undefined,
      });
      router.replace("/login");
    } catch (requestError) {
      setError(getAuthErrorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      <div className="mb-8 text-center sm:text-left">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Create your account</h1>
        <p className="mt-2 text-sm text-slate-600">Set up your personal InvoiceFlow AI workspace.</p>
      </div>
      
      <form className="mt-7" onSubmit={handleSubmit} noValidate>
        <div className="space-y-6">
          {/* Personal Info */}
          <div>
            <h2 className="text-sm font-semibold text-slate-900 mb-4 border-b pb-2">Personal Information</h2>
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="First name" value={form.firstName} onChange={(value) => updateField("firstName", value)} required />
              <Field label="Last name" value={form.lastName} onChange={(value) => updateField("lastName", value)} required />
              <Field className="sm:col-span-2" label="Company name" value={form.companyName} onChange={(value) => updateField("companyName", value)} />
            </div>
          </div>

          {/* Contact Info */}
          <div>
            <h2 className="text-sm font-semibold text-slate-900 mb-4 border-b pb-2">Contact Details</h2>
            <div className="grid gap-5 sm:grid-cols-2">
              <Field className="sm:col-span-2" label="Email" value={form.email} onChange={(value) => updateField("email", value)} required type="email" />
              <Field label="Phone number" value={form.phoneNumber} onChange={(value) => updateField("phoneNumber", value)} type="tel" />
              <Field label="Country" value={form.country} onChange={(value) => updateField("country", value)} />
            </div>
          </div>

          {/* Security */}
          <div>
            <h2 className="text-sm font-semibold text-slate-900 mb-4 border-b pb-2">Security</h2>
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Password" value={form.password} onChange={(value) => updateField("password", value)} required type="password" />
              <Field label="Confirm password" value={form.confirmPassword} onChange={(value) => updateField("confirmPassword", value)} required type="password" />
            </div>
          </div>
        </div>

        <div className="mt-8 space-y-4">
          {error && <p aria-live="polite" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-800">{error}</p>}
          <Button className="w-full gap-2" disabled={isSubmitting} type="submit">
            <UserPlus className="h-4 w-4" />
            {isSubmitting ? "Creating account…" : "Create account"}
          </Button>
        </div>
      </form>
      
      <div className="mt-8 flex items-center before:mt-0.5 before:flex-1 before:border-t before:border-slate-200 after:mt-0.5 after:flex-1 after:border-t after:border-slate-200">
        <span className="mx-4 mb-0 text-center text-sm font-medium text-slate-500">Already have an account?</span>
      </div>
      
      <div className="mt-6 text-center">
        <Link className="text-sm font-semibold text-teal-600 hover:text-teal-700 hover:underline" href="/login">Log in</Link>
      </div>
    </>
  );
}

function Field({ className, label, onChange, required, type = "text", value }: {
  className?: string; label: string; onChange: (value: string) => void; required?: boolean; type?: string; value: string;
}) {
  const id = label.toLowerCase().replaceAll(" ", "-");
  return <div className={`space-y-2 ${className ?? ""}`}><Label htmlFor={id}>{label}</Label><Input autoComplete={type === "password" ? "new-password" : undefined} id={id} onChange={(event) => onChange(event.target.value)} required={required} type={type} value={value} /></div>;
}
