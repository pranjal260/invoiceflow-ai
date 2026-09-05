"use client";

import { useCallback, useRef, useState } from "react";
import { UploadCloud, FileText, Eye, Trash2, Sparkles, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { SuccessFeedback } from "@/components/ui/feedback";
import { ApiClientError } from "@/services/api-client";
import { invoiceService } from "@/services/invoice-service";
import type { Invoice, InvoiceItem } from "@/types/api";

const ACCEPTED_TYPES = ["application/pdf", "image/jpeg", "image/png"];
const ACCEPTED_EXTENSIONS = ".pdf,.jpg,.jpeg,.png";
const MAX_SIZE_BYTES = 10 * 1024 * 1024;
const MAX_SIZE_LABEL = "10 MB";

type Props = {
  invoice: Invoice;
  onInvoiceUpdate: (invoice: Invoice) => void;
  onItemsUpdate: (items: InvoiceItem[]) => void;
};

function getDocumentErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    if (error.details.status === 502) {
      return "AI extraction failed. Please check the document and try again.";
    }
    if (error.details.status === 409) {
      return error.details.message;
    }
    if (error.details.status === 404) {
      return "Invoice or document not found.";
    }
    return error.details.message;
  }
  return "Something went wrong. Please try again.";
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function DocumentSection({ invoice, onInvoiceUpdate, onItemsUpdate }: Props) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [extracting, setExtracting] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [viewing, setViewing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [confirmExtract, setConfirmExtract] = useState(false);

  const hasDocument = Boolean(invoice.fileUrl);
  const busy = uploading || extracting || deleting || viewing;

  const clearFile = useCallback(() => {
    setSelectedFile(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  }, []);

  function handleFileSelect(event: React.ChangeEvent<HTMLInputElement>) {
    setError(null);
    setSuccess(null);
    const file = event.target.files?.[0];
    if (!file) return;

    if (!ACCEPTED_TYPES.includes(file.type)) {
      setError("Unsupported file type. Please select a PDF, JPG, or PNG file.");
      clearFile();
      return;
    }

    if (file.size > MAX_SIZE_BYTES) {
      setError(`File is too large (${formatFileSize(file.size)}). Maximum size is ${MAX_SIZE_LABEL}.`);
      clearFile();
      return;
    }

    if (file.size === 0) {
      setError("File is empty. Please select a valid document.");
      clearFile();
      return;
    }

    setSelectedFile(file);
  }

  async function uploadDocument() {
    if (!selectedFile || busy) return;
    setError(null);
    setSuccess(null);
    setUploading(true);
    try {
      const updated = await invoiceService.uploadDocument(invoice.id, selectedFile);
      onInvoiceUpdate(updated);
      clearFile();
      setSuccess("Document uploaded successfully.");
    } catch (e) {
      setError(getDocumentErrorMessage(e));
    } finally {
      setUploading(false);
    }
  }

  async function viewDocument() {
    if (busy) return;
    setError(null);
    setViewing(true);
    try {
      const doc = await invoiceService.getDocument(invoice.id);
      const opened = window.open(doc.url, "_blank", "noopener,noreferrer");
      if (!opened) {
        setError("Unable to open document. Please allow pop-ups for this site and try again.");
      }
    } catch (e) {
      setError(getDocumentErrorMessage(e));
    } finally {
      setViewing(false);
    }
  }

  async function deleteDocument() {
    if (busy) return;
    setError(null);
    setSuccess(null);
    setDeleting(true);
    try {
      await invoiceService.deleteDocument(invoice.id);
      const refreshed = await invoiceService.getInvoice(invoice.id);
      onInvoiceUpdate(refreshed);
      setConfirmDelete(false);
      clearFile();
      setSuccess("Document deleted successfully.");
    } catch (e) {
      setError(getDocumentErrorMessage(e));
      setConfirmDelete(false);
    } finally {
      setDeleting(false);
    }
  }

  async function extractWithAi() {
    if (busy) return;
    setError(null);
    setSuccess(null);
    setConfirmExtract(false);
    setExtracting(true);
    try {
      await invoiceService.extract(invoice.id);
      const [refreshedInvoice, refreshedItems] = await Promise.all([
        invoiceService.getInvoice(invoice.id),
        invoiceService.getInvoiceItems(invoice.id),
      ]);
      onInvoiceUpdate(refreshedInvoice);
      onItemsUpdate(refreshedItems);
      setSuccess("Invoice extracted successfully.");
    } catch (e) {
      setError(getDocumentErrorMessage(e));
    } finally {
      setExtracting(false);
    }
  }

  return (
    <Card className="mt-6">
      <h2 className="text-lg font-semibold text-slate-900 mb-4">Invoice document</h2>

      {success && (
        <div className="mb-4">
          <SuccessFeedback message={success} onDismiss={() => setSuccess(null)} />
        </div>
      )}

      {error && (
        <p className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>
      )}

      {extracting && (
        <div className="mb-4 flex items-start gap-4 rounded-lg bg-teal-50 border border-teal-100 p-4">
          <Loader2 className="h-5 w-5 text-teal-600 animate-spin mt-0.5" />
          <div>
            <p className="font-medium text-teal-900">Extracting invoice with AI…</p>
            <p className="mt-1 text-sm text-teal-700">
              Gemini is reading the document and extracting invoice details. This may take a moment.
            </p>
          </div>
        </div>
      )}

      {!hasDocument && !extracting && (
        <div className="mt-2">
          <div className="rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 p-8 text-center transition-colors hover:border-teal-300 hover:bg-teal-50">
            <UploadCloud className="mx-auto h-10 w-10 text-slate-400" />
            <h3 className="mt-4 text-sm font-semibold text-slate-900">Upload invoice document</h3>
            <p className="mt-1 text-xs text-slate-500">Supports PDF, JPEG, PNG up to {MAX_SIZE_LABEL}</p>
            
            <div className="mt-6 flex flex-col items-center justify-center gap-3">
              <input
                ref={fileInputRef}
                accept={ACCEPTED_EXTENSIONS}
                className="hidden"
                id="doc-upload-input"
                onChange={handleFileSelect}
                type="file"
              />
              {!selectedFile && (
                <Button disabled={busy} onClick={() => fileInputRef.current?.click()} type="button">
                  Choose file
                </Button>
              )}
              {selectedFile && (
                <div className="flex flex-col items-center gap-3">
                  <span className="text-sm font-medium text-teal-700 bg-teal-50 px-3 py-1 rounded-full border border-teal-100">
                    {selectedFile.name} ({formatFileSize(selectedFile.size)})
                  </span>
                  <div className="flex gap-2">
                    <Button disabled={uploading} onClick={() => void uploadDocument()} className="gap-2">
                      <UploadCloud className="h-4 w-4" />
                      {uploading ? "Uploading…" : "Upload document"}
                    </Button>
                    <Button disabled={uploading} onClick={clearFile} variant="ghost">
                      Cancel
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {hasDocument && !extracting && (
        <div className="mt-2 space-y-6">
          <div className="flex items-center justify-between rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-teal-50 text-teal-600">
                <FileText className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-medium text-slate-900">Document attached</p>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500" aria-hidden="true" />
                  <span className="text-xs text-slate-500">Available</span>
                </div>
              </div>
            </div>
            
            <div className="flex items-center gap-2">
              <Button disabled={busy} onClick={() => void viewDocument()} variant="secondary" className="gap-2">
                <Eye className="h-4 w-4" />
                {viewing ? "Opening…" : "View"}
              </Button>

              <input
                ref={fileInputRef}
                accept={ACCEPTED_EXTENSIONS}
                className="hidden"
                id="doc-replace-input"
                onChange={handleFileSelect}
                type="file"
              />
              {!selectedFile && (
                <Button disabled={busy} onClick={() => fileInputRef.current?.click()} variant="secondary" className="gap-2">
                  <UploadCloud className="h-4 w-4" />
                  Replace
                </Button>
              )}

              <Button disabled={busy} onClick={() => setConfirmDelete(true)} variant="danger" className="p-2" aria-label="Delete document">
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {selectedFile && (
            <div className="flex items-center justify-between rounded-lg bg-slate-50 p-4 border border-slate-200">
              <span className="text-sm font-medium text-slate-700">
                {selectedFile.name} ({formatFileSize(selectedFile.size)})
              </span>
              <div className="flex items-center gap-2">
                <Button disabled={uploading} onClick={() => void uploadDocument()} className="gap-2">
                  <UploadCloud className="h-4 w-4" />
                  {uploading ? "Uploading…" : "Upload replacement"}
                </Button>
                <Button disabled={uploading} onClick={clearFile} variant="ghost">
                  Cancel
                </Button>
              </div>
            </div>
          )}

          <div className="rounded-xl border border-indigo-100 bg-gradient-to-r from-indigo-50 to-blue-50 p-5 shadow-sm">
            <div className="flex items-start gap-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-100 text-indigo-600 shrink-0">
                <Sparkles className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-indigo-900">Extract with AI</h3>
                <p className="mt-1 text-sm text-indigo-700">
                  Let Gemini automatically read the document and fill in all the invoice details and line items for you.
                </p>
                <Button className="mt-3 gap-2 bg-indigo-600 hover:bg-indigo-700 text-white" disabled={busy} onClick={() => setConfirmExtract(true)}>
                  <Sparkles className="h-4 w-4" />
                  Extract with AI
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      <Dialog open={confirmDelete} title="Delete invoice document?" onClose={() => !deleting && setConfirmDelete(false)}>
        <p className="text-sm text-slate-600">
          This will permanently remove the attached document. This action cannot be undone.
        </p>
        <div className="mt-5 flex justify-end gap-3">
          <Button disabled={deleting} onClick={() => setConfirmDelete(false)} variant="secondary">
            Cancel
          </Button>
          <Button disabled={deleting} onClick={() => void deleteDocument()} variant="danger">
            {deleting ? "Deleting…" : "Delete document"}
          </Button>
        </div>
      </Dialog>

      <Dialog open={confirmExtract} title="Extract invoice with AI?" onClose={() => setConfirmExtract(false)}>
        <p className="text-sm text-slate-600">
          Gemini will read the attached document and update this invoice&apos;s details
          and line items. <strong>Existing invoice values and line items will be replaced.</strong>
        </p>
        <p className="mt-2 text-sm text-slate-500">
          This is not a preview. Changes are applied immediately.
        </p>
        <div className="mt-5 flex justify-end gap-3">
          <Button onClick={() => setConfirmExtract(false)} variant="secondary">
            Cancel
          </Button>
          <Button onClick={() => void extractWithAi()} className="bg-indigo-600 hover:bg-indigo-700 text-white gap-2">
            <Sparkles className="h-4 w-4" /> Extract now
          </Button>
        </div>
      </Dialog>
    </Card>
  );
}
