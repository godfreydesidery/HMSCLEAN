package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.creditnote.application.CreditNoteDtos.CreditNoteDto;
import com.otapp.hmis.engine.billing.creditnote.application.CreditNoteService;
import com.otapp.hmis.engine.billing.creditnote.domain.CreditNote;
import com.otapp.hmis.engine.billing.creditnote.domain.CreditNoteReason;
import com.otapp.hmis.engine.billing.creditnote.domain.CreditNoteRepository;
import com.otapp.hmis.engine.billing.creditnote.infrastructure.CreditNoteNumberGenerator;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
import com.otapp.hmis.engine.billing.refund.application.RefundDtos.CreateRefundRequest;
import com.otapp.hmis.engine.billing.refund.application.RefundService;
import com.otapp.hmis.engine.billing.refund.domain.RefundReason;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Billing-side cascade for the legacy consultation cancel / sign-out. Driven by
 * encounter-published events (billing depends on encounter, never the reverse).
 *
 * <ul>
 *   <li><b>Cancel</b> (legacy cancel_consultation step 4): refund any received
 *       payment, raise a PENDING credit-note audit record for the original
 *       amount, then cancel the (now-unpaid) consultation invoice.</li>
 *   <li><b>Sign-out</b> (legacy free_consultation step 5): void the unpaid
 *       invoice lines for downstream orders / prescriptions the encounter side
 *       has just cancelled, recomputing the invoice subtotal. PAID invoices and
 *       PAID/settled lines are left intact.</li>
 * </ul>
 *
 * <p>Money math stays sound: the refund returns the cash (reduces totalPaid),
 * the credit note is an audit reference only (NOT applied to the balance, to
 * avoid double-reducing), and the invoice is then cancelled. Reuses
 * {@link RefundService} and the existing {@link Invoice} reversal primitives.
 */
@Service
@RequiredArgsConstructor
public class ConsultationReversalService {

    private static final String CANCEL_DESC = "Cancelled consultation";
    private static final String SYSTEM_USER = "system";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final CreditNoteNumberGenerator creditNoteNumberGenerator;
    private final CreditNoteService creditNoteService;
    private final RefundService refundService;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final PrescriptionRepository prescriptionRepository;

    /** Reversal credit notes raised against a consultation's invoice (newest first). */
    @Transactional(readOnly = true)
    public List<CreditNoteDto> listCreditNotesForConsultation(String consultationUid) {
        Invoice invoice = invoiceRepository.findByConsultationUid(consultationUid).orElse(null);
        return invoice == null ? List.of() : creditNoteService.listForInvoice(invoice.getUid());
    }

    /**
     * Reverse the consultation fee on cancel. Idempotent — a no-op if the
     * invoice is absent or already CANCELLED.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reverseConsultationFee(String consultationUid) {
        Invoice invoice = invoiceRepository.findByConsultationUid(consultationUid).orElse(null);
        if (invoice == null || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return;
        }

        BigDecimal paid = invoice.getTotalPaid();

        // Legacy cancel_consultation raised a credit note ONLY when money had
        // actually been collected (PatientPaymentDetail RECEIVED) — the credit
        // note mirrors a real refund. An unpaid cancellation just voids the
        // invoice: no refund, no credit note.
        if (paid.signum() > 0) {
            // 1) Return the received cash (rolls the invoice back from PAID).
            refundService.raise(invoice.getUid(), new CreateRefundRequest(
                    paid, PaymentMethod.CASH, RefundReason.CANCELLATION, CANCEL_DESC, null));
            // 2) PENDING credit-note audit reference for the refund — NOT applied
            //    to the balance (the refund already returned the cash; applying it
            //    too would double-reduce).
            creditNoteRepository.save(new CreditNote(
                    creditNoteNumberGenerator.next(),
                    invoice.getUid(),
                    paid,
                    invoice.getCurrency(),
                    CreditNoteReason.SERVICE_NOT_RENDERED,
                    CANCEL_DESC,
                    SYSTEM_USER));
        }

        // 3) Void the invoice (it is no longer PAID after the refund).
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.cancel(CANCEL_DESC);
        }
    }

    /**
     * On sign-out, remove the unpaid invoice lines whose referenced order / Rx
     * was just cancelled encounter-side, and recompute the subtotal. Skips PAID
     * invoices entirely and never removes a line for a still-live (non-cancelled)
     * order/Rx. Idempotent.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void voidUnpaidDownstreamLines(String consultationUid) {
        Invoice invoice = invoiceRepository.findByConsultationUid(consultationUid).orElse(null);
        if (invoice == null
                || invoice.getStatus() == InvoiceStatus.PAID
                || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return;
        }

        List<InvoiceLine> lines = invoiceLineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(invoice.getUid());
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean changed = false;
        for (InvoiceLine line : lines) {
            if (isVoidableDownstreamLine(line)) {
                invoiceLineRepository.delete(line);
                changed = true;
                continue;
            }
            subtotal = subtotal.add(line.getAmount());
        }
        if (changed) {
            invoice.reduceSubtotalTo(subtotal);
        }
    }

    private boolean isVoidableDownstreamLine(InvoiceLine line) {
        if (line.getReferenceUid() == null) {
            return false;
        }
        return switch (line.getKind()) {
            case LAB_TEST, RADIOLOGY, PROCEDURE -> isCancelledOrder(line.getReferenceUid());
            case MEDICINE -> isCancelledPrescription(line.getReferenceUid());
            default -> false; // CONSULTATION / WARD / REGISTRATION / CONSUMABLE — never swept here
        };
    }

    private boolean isCancelledOrder(String orderUid) {
        return clinicalOrderRepository.findByUid(orderUid)
                .map(ClinicalOrder::getStatus)
                .map(s -> s == ClinicalOrderStatus.CANCELLED)
                .orElse(false);
    }

    private boolean isCancelledPrescription(String prescriptionUid) {
        return prescriptionRepository.findByUid(prescriptionUid)
                .map(Prescription::getStatus)
                .map(s -> s == PrescriptionStatus.CANCELLED)
                .orElse(false);
    }
}
