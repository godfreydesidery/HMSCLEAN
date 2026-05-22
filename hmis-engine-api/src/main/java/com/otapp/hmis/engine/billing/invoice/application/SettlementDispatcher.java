package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceScope;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
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

    /**
     * Dispatch settlement signals for a fully-settled invoice. No-op if the
     * invoice is not PAID yet. Marks the consultation fee settled (CONSULTATION
     * scope) and every dispensed prescription billed on the invoice.
     */
    public void onInvoiceMaybeSettled(Invoice invoice) {
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
