package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.billing.invoice.infrastructure.InvoiceNumberGenerator;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bills a consultation-bound clinical order / prescription onto the consultation
 * invoice <em>at order time</em>, so a CASH patient pays before the service is
 * rendered (PROCESS_MISMATCHES.md M13). Runs after-commit in a new transaction
 * (mirrors {@link ConsultationFeeService}). Idempotent — a line is added at most
 * once per order/prescription. Non-CASH or zero-priced items are marked settled
 * immediately (legacy COVERED); CASH items settle when the invoice is paid (via
 * {@link SettlementDispatcher}).
 */
@Service
@RequiredArgsConstructor
public class ServiceChargeService {

    private static final String DEFAULT_CURRENCY = "TZS";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final PriceLookup priceLookup;
    private final ConsultationRepository consultationRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void billOrder(String orderUid) {
        ClinicalOrder order = clinicalOrderRepository.findByUid(orderUid).orElse(null);
        if (order == null || order.getConsultationUid() == null) return; // outsider / missing — not billed here
        Consultation consultation = consultationRepository.findByUid(order.getConsultationUid()).orElse(null);
        if (consultation == null) return;

        Invoice invoice = ensureConsultationInvoice(consultation);
        if (alreadyBilled(invoice.getUid(), order.getUid())) return;

        ServiceKind serviceKind = switch (order.getKind()) {
            case LAB_TEST  -> ServiceKind.LAB_TEST;
            case RADIOLOGY -> ServiceKind.RADIOLOGY;
            case PROCEDURE -> ServiceKind.PROCEDURE;
        };
        InvoiceLineKind lineKind = switch (order.getKind()) {
            case LAB_TEST  -> InvoiceLineKind.LAB_TEST;
            case RADIOLOGY -> InvoiceLineKind.RADIOLOGY;
            case PROCEDURE -> InvoiceLineKind.PROCEDURE;
        };
        PriceLookup.Resolved r = priceLookup.resolve(
                serviceKind, order.getServiceUid(), consultation.getInsurancePlanUid(), invoice.getCurrency());
        addLine(invoice, lineKind, order.getServiceUid(), order.getUid(),
                order.getKind() + " (" + order.getOrderNo() + ")", BigDecimal.ONE, r.amount());

        if (consultation.getPaymentType() != PaymentType.CASH || r.amount().signum() == 0) {
            order.markSettled();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void billPrescription(String prescriptionUid) {
        Prescription rx = prescriptionRepository.findByUid(prescriptionUid).orElse(null);
        if (rx == null || rx.getConsultationUid() == null) return;
        Consultation consultation = consultationRepository.findByUid(rx.getConsultationUid()).orElse(null);
        if (consultation == null) return;

        Invoice invoice = ensureConsultationInvoice(consultation);
        if (alreadyBilled(invoice.getUid(), rx.getUid())) return;

        Medicine medicine = medicineRepository.findByUid(rx.getMedicineUid()).orElse(null);
        BigDecimal qty = rx.getQuantity() == null ? BigDecimal.ONE : BigDecimal.valueOf(rx.getQuantity());
        PriceLookup.Resolved r = priceLookup.resolve(
                ServiceKind.MEDICINE, rx.getMedicineUid(), consultation.getInsurancePlanUid(), invoice.getCurrency());
        String desc = (medicine == null ? rx.getMedicineUid() : medicine.getName())
                + " · " + rx.getDose() + " (" + rx.getPrescriptionNo() + ")";
        addLine(invoice, InvoiceLineKind.MEDICINE, rx.getMedicineUid(), rx.getUid(), desc, qty, r.amount());

        if (consultation.getPaymentType() != PaymentType.CASH || r.amount().signum() == 0) {
            rx.markSettled();
        }
    }

    // ----- helpers -----------------------------------------------------------

    private Invoice ensureConsultationInvoice(Consultation consultation) {
        Invoice invoice = invoiceRepository.findByConsultationUid(consultation.getUid()).orElse(null);
        if (invoice == null) {
            invoice = Invoice.forConsultation(
                    invoiceNumberGenerator.next(),
                    consultation.getUid(),
                    consultation.getPatientUid(),
                    consultation.getPaymentType(),
                    consultation.getInsurancePlanUid(),
                    DEFAULT_CURRENCY);
            invoiceRepository.save(invoice);
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            invoice.issue();
        }
        return invoice;
    }

    private boolean alreadyBilled(String invoiceUid, String referenceUid) {
        return invoiceLineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(invoiceUid).stream()
                .anyMatch(l -> referenceUid.equals(l.getReferenceUid()));
    }

    private void addLine(Invoice invoice, InvoiceLineKind kind, String serviceUid, String referenceUid,
                         String description, BigDecimal qty, BigDecimal unitPrice) {
        BigDecimal amount = unitPrice.multiply(qty);
        invoiceLineRepository.save(new InvoiceLine(
                invoice.getUid(), kind, serviceUid, referenceUid, description, qty, unitPrice, amount));
        invoice.setSubtotal(invoice.getSubtotal().add(amount));
    }
}
