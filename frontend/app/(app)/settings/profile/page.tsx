"use client";

import { FormEvent, useEffect, useState } from "react";
import { User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { SuccessFeedback } from "@/components/ui/feedback";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/context/AuthContext";
import { getAuthErrorMessage } from "@/lib/auth-errors";
import { userService, type UserProfileUpdateRequest } from "@/services/user-service";

const emptyForm: UserProfileUpdateRequest = { firstName: "", lastName: "", companyName: "", email: "", phoneNumber: "", country: "" };

export default function ProfileSettingsPage() {
  const { isLoading, updateUser, user } = useAuth();
  const [form, setForm] = useState<UserProfileUpdateRequest>(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setForm({ firstName: user.firstName, lastName: user.lastName, companyName: user.companyName ?? "", email: user.email, phoneNumber: user.phoneNumber ?? "", country: user.country ?? "" });
    }
  }, [user]);

  if (isLoading || !user) {
    return <section className="mx-auto max-w-3xl"><Skeleton className="h-8 w-48" /><Skeleton className="mt-6 h-96 w-full rounded-xl" /></section>;
  }

  function updateField(field: keyof UserProfileUpdateRequest, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (!form.firstName.trim() || !form.lastName.trim() || !form.email.trim()) {
      setError("First name, last name, and email are required.");
      return;
    }

    setIsSaving(true);
    try {
      const updatedUser = await userService.updateProfile({
        firstName: form.firstName.trim(), lastName: form.lastName.trim(), email: form.email.trim(),
        companyName: form.companyName?.trim() || undefined, phoneNumber: form.phoneNumber?.trim() || undefined,
        country: form.country?.trim() || undefined,
      });
      updateUser(updatedUser);
      setSuccess("Profile updated successfully.");
    } catch (requestError) {
      setError(getAuthErrorMessage(requestError));
    } finally {
      setIsSaving(false);
    }
  }

  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();

  return (
    <section className="mx-auto max-w-3xl animate-fade-in">
      <div className="flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-100 text-teal-700">
          <User className="h-6 w-6" />
        </div>
        <div>
          <p className="text-sm font-medium text-teal-700">Settings</p>
          <h1 className="mt-1 text-2xl font-semibold text-slate-950">Profile</h1>
          <p className="mt-1 text-sm text-slate-600">Update the information associated with your account.</p>
        </div>
      </div>

      <Card className="mt-8">
        <div className="mb-6 flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-slate-200 text-xl font-semibold text-slate-700">
            {initials}
          </div>
          <div>
            <h2 className="font-semibold text-slate-900">{user.firstName} {user.lastName}</h2>
            <p className="text-sm text-slate-500">{user.email}</p>
          </div>
        </div>

        <form className="grid gap-6 sm:grid-cols-2" onSubmit={handleSubmit} noValidate>
          <div className="space-y-4 sm:col-span-2 sm:grid sm:grid-cols-2 sm:gap-6 sm:space-y-0">
            <ProfileField label="First name" onChange={(value) => updateField("firstName", value)} required value={form.firstName} />
            <ProfileField label="Last name" onChange={(value) => updateField("lastName", value)} required value={form.lastName} />
          </div>
          
          <div className="sm:col-span-2">
            <ProfileField label="Email" onChange={(value) => updateField("email", value)} required type="email" value={form.email} />
          </div>

          <div className="border-t pt-6 sm:col-span-2 sm:grid sm:grid-cols-2 sm:gap-6">
            <ProfileField label="Company name" onChange={(value) => updateField("companyName", value)} value={form.companyName ?? ""} />
            <div className="mt-4 sm:mt-0">
              <ProfileField label="Phone number" onChange={(value) => updateField("phoneNumber", value)} type="tel" value={form.phoneNumber ?? ""} />
            </div>
            <div className="mt-4 sm:col-span-2">
              <ProfileField label="Country" onChange={(value) => updateField("country", value)} value={form.country ?? ""} />
            </div>
          </div>

          {error && <p aria-live="polite" className="rounded-lg bg-red-50 p-3 text-sm font-medium text-red-800 sm:col-span-2">{error}</p>}
          {success && <div className="sm:col-span-2"><SuccessFeedback message={success} onDismiss={() => setSuccess(null)} /></div>}
          
          <div className="sm:col-span-2 flex justify-end">
            <Button disabled={isSaving} type="submit">{isSaving ? "Saving…" : "Save changes"}</Button>
          </div>
        </form>
      </Card>
    </section>
  );
}

function ProfileField({ label, onChange, required, type = "text", value }: { label: string; onChange: (value: string) => void; required?: boolean; type?: string; value: string }) {
  const id = `profile-${label.toLowerCase().replaceAll(" ", "-")}`;
  return <div className="space-y-2"><Label htmlFor={id}>{label}</Label><Input id={id} maxLength={label === "Company name" ? 150 : label === "Phone number" ? 30 : label === "Country" ? 100 : label === "Email" ? 254 : 100} onChange={(event) => onChange(event.target.value)} required={required} type={type} value={value} /></div>;
}
