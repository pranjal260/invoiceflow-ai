import Link from "next/link";
import { User, Shield } from "lucide-react";
import { Card } from "@/components/ui/card";

export default function SettingsPage() {
  return (
    <section className="mx-auto max-w-4xl animate-fade-in">
      <p className="text-sm font-medium text-teal-700">Settings</p>
      <h1 className="mt-1 text-2xl font-semibold tracking-tight sm:text-3xl">Settings Hub</h1>
      <p className="mt-1 text-sm text-slate-500">Manage your profile and security settings.</p>

      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        <Link href="/settings/profile" className="group">
          <Card className="flex h-full flex-col transition-all hover:border-teal-200 hover:shadow-md group-hover:bg-slate-50">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-100 text-teal-700">
                <User className="h-6 w-6" />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-slate-900 group-hover:text-teal-700 transition-colors">Profile</h2>
                <p className="text-sm text-slate-500">Update your personal and company information.</p>
              </div>
            </div>
          </Card>
        </Link>
        
        <Link href="/settings/security" className="group">
          <Card className="flex h-full flex-col transition-all hover:border-teal-200 hover:shadow-md group-hover:bg-slate-50">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-slate-100 text-slate-700 group-hover:bg-teal-100 group-hover:text-teal-700 transition-colors">
                <Shield className="h-6 w-6" />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-slate-900 group-hover:text-teal-700 transition-colors">Security</h2>
                <p className="text-sm text-slate-500">Change your password and manage security settings.</p>
              </div>
            </div>
          </Card>
        </Link>
      </div>
    </section>
  );
}
