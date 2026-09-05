"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { FolderOpen, Plus, Pencil, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { SuccessFeedback } from "@/components/ui/feedback";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { getAuthErrorMessage } from "@/lib/auth-errors";
import { categoryService, type CategoryRequest } from "@/services/category-service";
import type { Category } from "@/types/api";

const initialForm: CategoryRequest = { name: "", description: "" };

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [form, setForm] = useState<CategoryRequest>(initialForm);
  const [editing, setEditing] = useState<Category | null>(null);
  const [deleting, setDeleting] = useState<Category | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const loadCategories = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setCategories(await categoryService.getCategories());
    } catch (requestError) {
      setError(getAuthErrorMessage(requestError));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { void loadCategories(); }, [loadCategories]);

  function beginCreate() {
    setError(null); setSuccess(null); setEditing(null); setForm(initialForm);
  }

  function beginEdit(category: Category) {
    setError(null); setSuccess(null); setEditing(category); setForm({ name: category.name, description: category.description ?? "" });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null); setSuccess(null);
    if (!form.name.trim()) {
      setError("Category name is required.");
      return;
    }
    setIsSaving(true);
    const request = { name: form.name.trim(), description: form.description?.trim() || undefined };
    try {
      if (editing) {
        const updated = await categoryService.updateCategory(editing.id, request);
        setCategories((current) => current.map((category) => category.id === updated.id ? updated : category));
        setSuccess("Category updated successfully.");
      } else {
        const created = await categoryService.createCategory(request);
        setCategories((current) => [...current, created].sort((left, right) => left.name.localeCompare(right.name)));
        setSuccess("Category created successfully.");
      }
      setForm(initialForm); setEditing(null);
    } catch (requestError) {
      setError(getAuthErrorMessage(requestError));
    } finally {
      setIsSaving(false);
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    setDeletingId(deleting.id); setError(null); setSuccess(null);
    try {
      await categoryService.deleteCategory(deleting.id);
      setCategories((current) => current.filter((category) => category.id !== deleting.id));
      setSuccess("Category deleted successfully.");
      setDeleting(null);
    } catch (requestError) {
      setError(getAuthErrorMessage(requestError));
      setDeleting(null);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <section className="mx-auto max-w-5xl animate-fade-in">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-100 text-teal-700">
            <FolderOpen className="h-6 w-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-teal-700">Organization</p>
            <h1 className="mt-1 text-2xl font-semibold text-slate-950">Categories</h1>
            <p className="mt-1 text-sm text-slate-600">Organize the financial records in your workspace.</p>
          </div>
        </div>
        <Button onClick={beginCreate} className="gap-2"><Plus className="h-4 w-4" /> New category</Button>
      </div>

      <Card className="mt-6">
        <form className="grid gap-4 sm:grid-cols-[1fr_2fr_auto]" onSubmit={handleSubmit} noValidate>
          <div className="space-y-2">
            <Label htmlFor="category-name">Category name</Label>
            <Input id="category-name" maxLength={100} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} required value={form.name} placeholder="e.g. Consulting" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="category-description">Description <span className="font-normal text-slate-400">(optional)</span></Label>
            <Input id="category-description" maxLength={500} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} value={form.description ?? ""} placeholder="Add a description" />
          </div>
          <div className="flex items-end gap-2 pb-0.5">
            <Button disabled={isSaving} type="submit">{isSaving ? "Saving…" : editing ? "Save" : "Create"}</Button>
            {editing && <Button disabled={isSaving} onClick={beginCreate} variant="secondary">Cancel</Button>}
          </div>
        </form>
      </Card>

      {error && <div className="mt-4"><ErrorState description={error} title="Unable to complete the category request" /></div>}
      {success && <div className="mt-4"><SuccessFeedback message={success} onDismiss={() => setSuccess(null)} /></div>}

      <div className="mt-6">
        {isLoading ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <Skeleton className="h-32 w-full rounded-xl" />
            <Skeleton className="h-32 w-full rounded-xl" />
          </div>
        ) : error && categories.length === 0 ? (
          <ErrorState description="Retry the page after checking your connection." title="Could not load categories" />
        ) : categories.length === 0 ? (
          <EmptyState description="Create a category to organize invoices and analytics." title="No categories yet" />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {categories.map((category) => (
              <Card className="flex min-h-[8rem] flex-col transition-shadow hover:shadow-md" key={category.id}>
                <div className="flex items-start gap-3">
                  <div className="mt-0.5 flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-slate-500">
                    <FolderOpen className="h-4 w-4" />
                  </div>
                  <div className="flex-1">
                    <h2 className="font-semibold text-slate-950">{category.name}</h2>
                    <p className="mt-1 text-sm text-slate-600 line-clamp-2">{category.description || "No description"}</p>
                  </div>
                </div>
                <div className="mt-auto pt-4 flex gap-3">
                  <button className="flex items-center gap-1.5 text-sm font-medium text-teal-700 hover:text-teal-900 disabled:opacity-50" disabled={Boolean(deletingId)} onClick={() => beginEdit(category)} type="button">
                    <Pencil className="h-3.5 w-3.5" /> Edit
                  </button>
                  <button className="flex items-center gap-1.5 text-sm font-medium text-red-700 hover:text-red-900 disabled:opacity-50" disabled={Boolean(deletingId)} onClick={() => setDeleting(category)} type="button">
                    <Trash2 className="h-3.5 w-3.5" /> Delete
                  </button>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      <Dialog onClose={() => !deletingId && setDeleting(null)} open={Boolean(deleting)} title="Delete category">
        <p className="text-sm text-slate-600">Delete <strong>{deleting?.name}</strong>? This cannot be undone.</p>
        <div className="mt-6 flex justify-end gap-3">
          <Button disabled={Boolean(deletingId)} onClick={() => setDeleting(null)} variant="secondary">Cancel</Button>
          <Button disabled={Boolean(deletingId)} onClick={confirmDelete} variant="danger">{deletingId ? "Deleting…" : "Delete category"}</Button>
        </div>
      </Dialog>
    </section>
  );
}
