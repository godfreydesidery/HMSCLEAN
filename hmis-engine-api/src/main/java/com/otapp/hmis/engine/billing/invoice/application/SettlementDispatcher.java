package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceScope;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionService;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationService;
import com.otapp.hmis.engine.encounter.order.application.ClinicalOrderService;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pushes "this charge is settled" from billing into the encounter module so its
 * role queues and gates can read a local flag instead of querying billing.
 *
 * <p>Direction is billing → encounter, which the modulith allows; the encounter
 * module never imports billing. Called synchronously inside the same transaction
 * as the payment / credit-note that settled the invoice, so the flag write is
 * atomic with the settlement. All target methods are idempotent.
 */
@Component
@RequiredArgsConstructor
public class SettlementDispatcher {

    private final InvoiceLineRepository invoiceLineRepository;
    private final ConsultationService consultationService;
    private final PrescriptionService prescriptionService;
    private final ClinicalOrderService clinicalOrderService;
    private final AdmissionService admissionService;

    /**
     * Dispatch settlement signals for an invoice. When PAID, marks the consultation
     * fee settled (CONSULTATION scope), the admission bills cleared (ADMISSION scope),
     * and every dispensed prescription / completed order billed on the invoice. When
     * not PAID (e.g. a refund rolled the invoice back to a positive balance), re-arms
     * the admission closure gate so an outstanding bill blocks discharge again.
     */
    public void onInvoiceMaybeSettled(Invoice invoice) {
        // Keep the admission bill-clearance (discharge) gate in sync with what this
        // invoice still owes, on EVERY state change. When the invoice is PAID, is
        // CANCELLED (the bill was voided), or has a non-positive balance, nothing
        // is owed and the gate is cleared so closure / discharge is allowed. A live
        // positive balance (ISSUED or PARTIALLY_PAID, including after a refund or a
        // partial credit) re-arms the gate so it blocks again. Cancelling or
        // re-pricing an admission invoice down to nothing must not leave the gate
        // stuck blocked, so cancel / overrideLinePrice now route through here too.
        if (invoice.getScope() == InvoiceScope.ADMISSION && invoice.getAdmissionUid() != null) {
            boolean nothingOwed = invoice.getStatus() == InvoiceStatus.PAID
                    || invoice.getStatus() == InvoiceStatus.CANCELLED
                    || invoice.balance().signum() <= 0;
            if (nothingOwed) {
                admissionService.markBillsCleared(invoice.getAdmissionUid());
            } else {
                admissionService.clearBillsCleared(invoice.getAdmissionUid());
            }
        }
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            return;
        }
        settle(invoice);
    }

    /** Mark the encounter aggregates referenced by a settled invoice. */
    private void settle(Invoice invoice) {
        if (invoice.getScope() == InvoiceScope.CONSULTATION && invoice.getConsultationUid() != null) {
            consultationService.markFeeSettled(invoice.getConsultationUid());
        }
        if (invoice.getScope() == InvoiceScope.ADMISSION && invoice.getAdmissionUid() != null) {
            admissionService.markBillsCleared(invoice.getAdmissionUid());
        }
        for (InvoiceLine line : invoiceLineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(invoice.getUid())) {
            if (line.getReferenceUid() == null) continue;
            switch (line.getKind()) {
                case MEDICINE -> prescriptionService.markSettled(line.getReferenceUid());
                case LAB_TEST, RADIOLOGY, PROCEDURE -> clinicalOrderService.markSettled(line.getReferenceUid());
                default -> { /* CONSULTATION / WARD / REGISTRATION / CONSUMABLE — no order-level flag */ }
            }
        }
    }
}
