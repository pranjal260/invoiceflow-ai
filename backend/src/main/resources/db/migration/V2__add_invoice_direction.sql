-- Existing records represent the original vendor/expense-focused MVP workflow.
-- They are classified as EXPENSE until users explicitly update their invoices.
ALTER TABLE invoices
    ADD COLUMN direction VARCHAR(20) NOT NULL DEFAULT 'EXPENSE';

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoices_direction CHECK (direction IN ('INCOME', 'EXPENSE'));

ALTER TABLE invoices
    ALTER COLUMN direction DROP DEFAULT;
