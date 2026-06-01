-- Line-level cashier payment (legacy PatientBill per-line settlement).
--
-- The new billing model settled money at the invoice level only: a payment landed
-- in invoice.total_paid and an order/Rx was released only when the WHOLE invoice
-- reached PAID. The legacy cashier instead settles per service line ("check to
-- pay") and releases each paid line's order on its own. To restore that, each line
-- now carries the cash applied to it; paid_amount == amount IS the per-line paid
-- flag (and composes with partial pay). COVERED lines are insurer-settled via
-- invoice.total_covered and never take cash, so their paid_amount stays 0.
ALTER TABLE invoice_line
    ADD COLUMN paid_amount NUMERIC(14, 2) NOT NULL DEFAULT 0;

-- Backfill: cash lines on already-PAID invoices are fully paid. COVERED lines are
-- left at 0 (the insurer settled them; paid_amount tracks cash only). Invoices that
-- carry a credit-note write-down are also skipped — credit notes are invoice-level
-- (not line-linked), so we cannot attribute how much of each line was cash vs
-- written down; setting paid_amount = amount there would overstate collected cash.
-- Those invoices are PAID (balance 0) so their lines never resurface at the till.
UPDATE invoice_line l
SET paid_amount = l.amount
FROM invoice i
WHERE l.invoice_uid = i.uid
  AND i.status = 'PAID'
  AND i.total_credited = 0
  AND l.coverage_status <> 'COVERED';
