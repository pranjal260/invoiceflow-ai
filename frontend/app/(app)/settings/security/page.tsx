"use client";

import { FormEvent, useState } from "react";
import { Shield } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { SuccessFeedback } from "@/components/ui/feedback";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getAuthErrorMessage } from "@/lib/auth-errors";
import { authService } from "@/services/auth-service";

export default function SecuritySettingsPage() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (!currentPassword || !newPassword || !confirmation) {
      setError("Complete all password fields.");
      return;
    }
    if (newPassword.length < 8 || newPassword.length > 72) {
      setError("New password must contain between 8 and 72 characters.");
      return;
    }
    if (newPassword !== confirmation) {
      setError("New password confirmation does not match.");
      return;
    }

    setIsSaving(true);
    try {
      await authService.changePassword({ currentPassword, newPassword });
      setCurrentPassword(""); setNewPassword(""); setConfirmation("");
      setSuccess("Password updated successfully.");
    } catch (requestError) {
      setError(getAuthErrorMessage(requestError));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className="mx-auto max-w-3xl animate-fade-in">
      <div className="flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
          <Shield className="h-6 w-6" />
        </div>
        <div>
          <p className="text-sm font-medium text-teal-700">Settings</p>
          <h1 className="mt-1 text-2xl font-semibold text-slate-950">Security</h1>
          <p className="mt-1 text-sm text-slate-600">Choose a strong, unique password for your account.</p>
        </div>
      </div>
      
      <Card className="mt-8 max-w-xl">
        <form className="space-y-6" onSubmit={handleSubmit} noValidate>
          <div className="space-y-4">
            <PasswordField label="Current password" onChange={setCurrentPassword} value={currentPassword} />
            <div className="border-t pt-4">
              <PasswordField label="New password" onChange={setNewPassword} value={newPassword} />
            </div>
            <PasswordField label="Confirm new password" onChange={setConfirmation} value={confirmation} />
          </div>
          
          {error && <p aria-live="polite" className="rounded-lg bg-red-50 p-3 text-sm font-medium text-red-800">{error}</p>}
          {success && <SuccessFeedback message={success} onDismiss={() => setSuccess(null)} />}
          
          <div className="flex justify-end">
            <Button disabled={isSaving} type="submit">{isSaving ? "Updating…" : "Update password"}</Button>
          </div>
        </form>
      </Card>
    </section>
  );
}

function PasswordField({ label, onChange, value }: { label: string; onChange: (value: string) => void; value: string }) {
  const id = label.toLowerCase().replaceAll(" ", "-");
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input autoComplete={label === "Current password" ? "current-password" : "new-password"} id={id} maxLength={72} onChange={(event) => onChange(event.target.value)} required type="password" value={value} />
    </div>
  );
}
