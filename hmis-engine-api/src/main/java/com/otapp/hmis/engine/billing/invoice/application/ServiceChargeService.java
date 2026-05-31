package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.application.CoverageResolver.CoverageResolution;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceScope;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.billing.invoice.domain.LineCoverageStatus;
import com.otapp.hmis.engine.billing.invoice.infrastructure.InvoiceNumberGenerator;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyService;
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
 * once per order/prescription.
 *
 * <p>Each line is payer-routed via {@link CoverageResolver} (faithful to legacy
 * {@code PatientServiceImpl}): a service the patient's plan COVERS is priced at
 * the plan ceiling, stamped with membership / payer plan, and settled by the
 * insurer up front; an insured-but-uncovered service on an admission invoice is
 * VERIFIED (owed, hospital accrues, not settled); everything else is UNPAID cash,
 * settled when the invoice is paid (via {@link SettlementDispatcher}). A
 * zero-priced cash line settles immediately. Coverage is per-service — having an
 * insurance plan does NOT auto-cover everything.
 */
@Service
@RequiredArgsConstructor
public class ServiceChargeService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final PriceLookup priceLookup;
    private final CoverageResolver coverageResolver;
    private final ConsultationRepository consultationRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final CurrencyService currencyService;

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
        // Cash baseline (plan-agnostic) is the price the patient owes when not covered.
        BigDecimal cashAmount = priceLookup
                .resolve(serviceKind, order.getServiceUid(), null, invoice.getCurrency()).amount();
        boolean settled = chargeServiceLine(invoice, serviceKind, lineKind, order.getServiceUid(), order.getUid(),
                order.getKind() + " (" + order.getOrderNo() + ")", BigDecimal.ONE, cashAmount, consultation.getPaymentType());

        if (settled) {
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
        BigDecimal cashUnit = priceLookup
                .resolve(ServiceKind.MEDICINE, rx.getMedicineUid(), null, invoice.getCurrency()).amount();
        String desc = (medicine == null ? rx.getMedicineUid() : medicine.getName())
                + " · " + rx.getDose() + " (" + rx.getPrescriptionNo() + ")";
        boolean settled = chargeServiceLine(invoice, ServiceKind.MEDICINE, InvoiceLineKind.MEDICINE,
                rx.getMedicineUid(), rx.getUid(), desc, qty, cashUnit, consultation.getPaymentType());

        if (settled) {
            rx.markSettled();
        }
    }

    // ----- helpers -----------------------------------------------------------

    /**
     * Charges one service line, routing the payer per the legacy
     * {@code PatientServiceImpl} accrual:
     *
     * <ul>
     *   <li><b>COVERED</b> — the patient's plan covers this service: the line is
     *       priced at the plan ceiling, stamped with the membership number +
     *       payer plan, and settled by the insurer. If the cash price exceeds the
     *       ceiling (a ward-style co-pay), a supplementary UNPAID top-up line is
     *       split off, linked to the principal — the patient/self-pay still owes
     *       the remainder.</li>
     *   <li><b>VERIFIED</b> — an insured patient whose plan does NOT cover the
     *       service, on an inpatient (ADMISSION) invoice: charged at cash price,
     *       owed (hospital accrues, insurer won't pay). Not settled.</li>
     *   <li><b>UNPAID</b> — cash, or insured-but-uncovered on a non-admission
     *       (outpatient) invoice: charged at cash price, owed. Settled only when
     *       zero-priced (nothing to collect).</li>
     * </ul>
     *
     * @return whether the encounter aggregate should be marked settled now.
     */
    @SuppressWarnings("java:S107")
    private boolean chargeServiceLine(Invoice invoice, ServiceKind serviceKind, InvoiceLineKind lineKind,
                                      String serviceUid, String referenceUid, String description,
                                      BigDecimal qty, BigDecimal cashUnit, PaymentType paymentType) {
        CoverageResolution coverage = coverageResolver.resolve(
                serviceKind, serviceUid, invoice.getInsurancePlanUid(), invoice.getPaymentType(),
                invoice.getPatientUid(), invoice.getCurrency(), cashUnit);

        if (coverage.covered()) {
            // Plan covers this service in full: price at the plan ceiling, stamp
            // membership / payer plan, insurer settles it up front. This service
            // only bills consultation lab / radiology / procedure / medicine, which
            // route COVERED-whole — the legacy ward co-pay supplementary split is an
            // admission-billing concern (no WARD line is ever charged here).
            addRoutedLine(invoice, lineKind, serviceUid, referenceUid, description, qty,
                    coverage.coveredAmount(), LineCoverageStatus.COVERED, coverage.membershipNo(), coverage.planUid());
            return true; // insurer settles the covered principal
        }

        // Not covered. VERIFIED on an admission invoice (still owed, hospital
        // accrues); UNPAID otherwise (cash / outpatient).
        LineCoverageStatus status = (paymentType != PaymentType.CASH && invoice.getScope() == InvoiceScope.ADMISSION)
                ? LineCoverageStatus.VERIFIED
                : LineCoverageStatus.UNPAID;
        addRoutedLine(invoice, lineKind, serviceUid, referenceUid, description, qty, cashUnit, status, null, null);
        // A zero-priced cash line has nothing to collect, so it settles up front
        // (preserves the prior zero-price short-circuit); VERIFIED never auto-settles.
        return status == LineCoverageStatus.UNPAID && cashUnit.signum() == 0;
    }

    private Invoice ensureConsultationInvoice(Consultation consultation) {
        Invoice invoice = invoiceRepository.findByConsultationUid(consultation.getUid()).orElse(null);
        if (invoice == null) {
            invoice = Invoice.forConsultation(
                    invoiceNumberGenerator.next(),
                    consultation.getUid(),
                    consultation.getPatientUid(),
                    consultation.getPaymentType(),
                    consultation.getInsurancePlanUid(),
                    currencyService.defaultCode());
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

    @SuppressWarnings("java:S107")
    private InvoiceLine addRoutedLine(Invoice invoice, InvoiceLineKind kind, String serviceUid, String referenceUid,
                                      String description, BigDecimal qty, BigDecimal unitPrice,
                                      LineCoverageStatus status, String membershipNo, String payerPlanUid) {
        BigDecimal amount = unitPrice.multiply(qty);
        InvoiceLine line = invoiceLineRepository.save(InvoiceLine.routed(
                invoice.getUid(), kind, serviceUid, referenceUid, description, qty, unitPrice, amount,
                status, membershipNo, payerPlanUid));
        invoice.setSubtotal(invoice.getSubtotal().add(amount));
        // A COVERED line is billed AND paid by the insurer up front (legacy gives
        // the covered bill balance 0): record the insurer payment so the covered
        // amount drops out of what the PATIENT owes — otherwise it stays in the
        // subtotal as patient balance and double-charges the patient.
        if (status == LineCoverageStatus.COVERED) {
            invoice.recordInsurerCovered(amount);
        }
        return line;
    }
}
