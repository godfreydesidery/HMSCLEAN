package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.CancelInvoiceRequest;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceDto;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceSummary;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.RecordPaymentRequest;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.billing.invoice.infrastructure.InvoiceNumberGenerator;
import com.otapp.hmis.engine.billing.payment.domain.Payment;
import com.otapp.hmis.engine.billing.payment.domain.PaymentRepository;
import com.otapp.hmis.engine.billing.payment.infrastructure.PaymentNumberGenerator;
import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableIssue;
import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableIssueRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestType;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestTypeRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureType;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureTypeRepository;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyType;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyTypeRepository;
import com.otapp.hmis.engine.masterdata.ward.domain.Ward;
import com.otapp.hmis.engine.masterdata.ward.domain.WardRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PatientType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final String DEFAULT_CURRENCY = "TZS";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final PaymentRepository paymentRepository;
    private final ConsultationRepository consultationRepository;
    private final AdmissionRepository admissionRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ConsumableIssueRepository consumableIssueRepository;
    private final ClinicRepository clinicRepository;
    private final WardRepository wardRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final RadiologyTypeRepository radiologyTypeRepository;
    private final ProcedureTypeRepository procedureTypeRepository;
    private final MedicineRepository medicineRepository;
    private final PatientRepository patientRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final PaymentNumberGenerator paymentNumberGenerator;
    private final PriceLookup priceLookup;
    private final InvoiceDtoAssembler dtoAssembler;
    private final SettlementDispatcher settlementDispatcher;

    /**
     * Tops up the consultation invoice with newly-billable work. The invoice
     * itself is seeded at booking by {@link ConsultationFeeService} (the
     * up-front consultation fee), so this is now <b>additive and idempotent</b>:
     * it ensures the invoice exists, adds the consultation-fee line if missing,
     * and appends any COMPLETED clinical order / DISPENSED prescription that
     * hasn't already been billed on this invoice. Charges accrue (legacy
     * behaviour); existing lines and recorded payments are never discarded, so
     * it is safe to call on an already-ISSUED / partially-paid invoice.
     */
    @Transactional
    public InvoiceDto generateForConsultation(String consultationUid) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));

        Invoice invoice = invoiceRepository.findByConsultationUid(consultationUid).orElse(null);
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

        // What's already on this invoice — never double-bill the same reference.
        Set<String> billed = new HashSet<>();
        boolean hasConsultationFeeLine = false;
        for (InvoiceLine existing : invoiceLineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(invoice.getUid())) {
            if (existing.getReferenceUid() != null) billed.add(existing.getReferenceUid());
            if (existing.getKind() == InvoiceLineKind.CONSULTATION) hasConsultationFeeLine = true;
        }

        List<InvoiceLine> lines = new ArrayList<>();
        BigDecimal subtotal = invoice.getSubtotal();
        String currency = invoice.getCurrency();

        // 1) Consultation fee — only if it wasn't already seeded at booking.
        Clinic clinic = clinicRepository.findByUid(consultation.getClinicUid()).orElse(null);
        if (clinic != null && !hasConsultationFeeLine) {
            PriceLookup.Resolved r = priceLookup.resolve(ServiceKind.CONSULTATION, clinic.getUid(), consultation.getInsurancePlanUid(), currency);
            currency = r.currency();
            lines.add(new InvoiceLine(invoice.getUid(), InvoiceLineKind.CONSULTATION,
                    clinic.getUid(), consultation.getUid(),
                    "Consultation — " + clinic.getName(),
                    BigDecimal.ONE, r.amount(), r.amount()));
            subtotal = subtotal.add(r.amount());
        }

        // 2) Clinical orders that are COMPLETED — actually billed only when service is rendered
        for (ClinicalOrder order : clinicalOrderRepository.findAllByConsultationUidOrderByRequestedAtDesc(consultationUid)) {
            if (order.getStatus() != ClinicalOrderStatus.COMPLETED) continue;
            if (billed.contains(order.getUid())) continue;
            InvoiceLineKind lineKind = mapKind(order.getKind());
            ServiceKind serviceKind = mapServiceKind(order.getKind());
            String serviceName = resolveOrderServiceName(order);
            PriceLookup.Resolved r = priceLookup.resolve(serviceKind, order.getServiceUid(), consultation.getInsurancePlanUid(), currency);
            currency = r.currency();
            lines.add(new InvoiceLine(invoice.getUid(), lineKind,
                    order.getServiceUid(), order.getUid(),
                    serviceName + " (" + order.getOrderNo() + ")",
                    BigDecimal.ONE, r.amount(), r.amount()));
            subtotal = subtotal.add(r.amount());
        }

        // 3) Prescriptions that are DISPENSED — quantity-based
        for (Prescription rx : prescriptionRepository.findAllByConsultationUidOrderByRequestedAtDesc(consultationUid)) {
            if (rx.getStatus() != PrescriptionStatus.SOLD) continue;
            if (billed.contains(rx.getUid())) continue;
            Medicine medicine = medicineRepository.findByUid(rx.getMedicineUid()).orElse(null);
            PriceLookup.Resolved r = priceLookup.resolve(ServiceKind.MEDICINE, rx.getMedicineUid(), consultation.getInsurancePlanUid(), currency);
            currency = r.currency();
            BigDecimal qty = rx.getQuantity() == null ? BigDecimal.ONE : BigDecimal.valueOf(rx.getQuantity());
            BigDecimal amount = r.amount().multiply(qty);
            String desc = (medicine == null ? rx.getMedicineUid() : medicine.getName())
                    + " · " + rx.getDose() + " · " + rx.getFrequency()
                    + " (" + rx.getPrescriptionNo() + ")";
            lines.add(new InvoiceLine(invoice.getUid(), InvoiceLineKind.MEDICINE,
                    rx.getMedicineUid(), rx.getUid(),
                    desc, qty, r.amount(), amount));
            subtotal = subtotal.add(amount);
        }

        invoiceLineRepository.saveAll(lines);
        invoice.setSubtotal(subtotal);
        invoice.setCurrency(currency);
        return toDto(invoice);
    }

    /**
     * Generates or regenerates the invoice for an admission. Pulls the
     * per-night ward rate, every COMPLETED clinical order raised during the
     * admission, and every DISPENSED prescription. Only allowed while the
     * invoice is still DRAFT.
     */
    @Transactional
    public InvoiceDto generateForAdmission(String admissionUid) {
        Admission admission = admissionRepository.findByUid(admissionUid)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + admissionUid));
        if (admission.getStatus() == AdmissionStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot bill a cancelled admission");
        }

        Invoice invoice = invoiceRepository.findByAdmissionUid(admissionUid).orElse(null);
        if (invoice != null && invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Invoice is already " + invoice.getStatus() + " and cannot be regenerated");
        }
        if (invoice == null) {
            invoice = Invoice.forAdmission(
                    invoiceNumberGenerator.next(),
                    admission.getUid(),
                    admission.getPatientUid(),
                    admission.getPaymentType(),
                    admission.getInsurancePlanUid(),
                    DEFAULT_CURRENCY);
            invoiceRepository.save(invoice);
        } else {
            invoiceLineRepository.deleteAllByInvoiceUid(invoice.getUid());
            invoice.setSubtotal(BigDecimal.ZERO);
            invoice.setPaymentType(admission.getPaymentType());
            invoice.setInsurancePlanUid(admission.getInsurancePlanUid());
        }

        List<InvoiceLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = invoice.getCurrency();

        // 1) Ward-day charge: days occupied between admit and discharge (or now).
        Ward ward = wardRepository.findByUid(admission.getWardUid()).orElse(null);
        if (ward != null) {
            BigDecimal days = ceilDaysBetween(admission.getAdmittedAt(),
                    admission.getDischargedAt() == null ? Instant.now() : admission.getDischargedAt());
            PriceLookup.Resolved r = priceLookup.resolve(ServiceKind.WARD, ward.getUid(), admission.getInsurancePlanUid(), currency);
            currency = r.currency();
            BigDecimal amount = r.amount().multiply(days);
            lines.add(new InvoiceLine(invoice.getUid(), InvoiceLineKind.WARD,
                    ward.getUid(), admission.getUid(),
                    "Ward stay — " + ward.getName() + " (" + days + " day" + (days.compareTo(BigDecimal.ONE) == 0 ? "" : "s") + ")",
                    days, r.amount(), amount));
            subtotal = subtotal.add(amount);
        }

        // 2) Patient consumable chart — every consumable issued against this
        // admission becomes a CONSUMABLE line using the snapshot unit cost.
        for (ConsumableIssue issue : consumableIssueRepository.findAllByAdmissionUidOrderByIssuedAtAsc(admissionUid)) {
            BigDecimal qty = BigDecimal.valueOf(issue.getQuantity());
            BigDecimal amount = issue.lineAmount();
            lines.add(new InvoiceLine(invoice.getUid(), InvoiceLineKind.CONSUMABLE,
                    issue.getConsumableUid(), issue.getUid(),
                    "Consumable — " + issue.getConsumableUid() + " ×" + issue.getQuantity(),
                    qty, issue.getUnitCost(), amount));
            subtotal = subtotal.add(amount);
        }

        // NOTE: clinical orders and prescriptions are still scoped to a
        // consultation, so they are billed on the source-consultation invoice
        // (if any), not the admission invoice. A future iteration can add
        // admission-scoped orders and pick them up here.

        invoiceLineRepository.saveAll(lines);
        invoice.setSubtotal(subtotal);
        invoice.setCurrency(currency);
        return toDto(invoice);
    }

    /**
     * Generate or regenerate the OUTSIDER (walk-in) invoice for a patient.
     * Picks up every COMPLETED outsider clinical order and every DISPENSED
     * outsider prescription for the patient that hasn't already been billed
     * on a prior invoice. Only one DRAFT outsider invoice exists per patient
     * at a time; once it's ISSUED, the next generate creates a fresh one.
     */
    @Transactional
    public InvoiceDto generateForOutsider(String patientUid) {
        Patient patient = patientRepository.findByUid(patientUid)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientUid));
        if (patient.getType() != PatientType.OUTSIDER) {
            throw new BusinessRuleException(
                    "Outsider invoices are only for OUTSIDER patients (current type: " + patient.getType() + ")");
        }
        if (!patient.isActive()) {
            throw new BusinessRuleException("Cannot bill an inactive patient");
        }

        Invoice invoice = invoiceRepository.findDraftOutsiderForPatient(patientUid).orElse(null);
        if (invoice == null) {
            invoice = Invoice.forOutsider(
                    invoiceNumberGenerator.next(),
                    patient.getUid(),
                    patient.getPaymentType(),
                    patient.getInsurancePlanUid(),
                    DEFAULT_CURRENCY);
            invoiceRepository.save(invoice);
        } else {
            invoiceLineRepository.deleteAllByInvoiceUid(invoice.getUid());
            invoice.setSubtotal(BigDecimal.ZERO);
            invoice.setPaymentType(patient.getPaymentType());
            invoice.setInsurancePlanUid(patient.getInsurancePlanUid());
        }

        // Already-billed work — anything sitting on prior (ISSUED/PAID) invoices
        // for this patient is skipped so we never double-bill.
        Set<String> alreadyBilled = new HashSet<>(invoiceLineRepository.findReferenceUidsBilledForPatient(patientUid));

        List<InvoiceLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = invoice.getCurrency();

        // 1) Outsider clinical orders that are COMPLETED and not yet billed.
        for (ClinicalOrder order :
                clinicalOrderRepository.findAllByPatientUidAndConsultationUidIsNullOrderByRequestedAtDesc(patientUid)) {
            if (order.getStatus() != ClinicalOrderStatus.COMPLETED) continue;
            if (alreadyBilled.contains(order.getUid())) continue;
            InvoiceLineKind lineKind = mapKind(order.getKind());
            ServiceKind serviceKind = mapServiceKind(order.getKind());
            String serviceName = resolveOrderServiceName(order);
            PriceLookup.Resolved r = priceLookup.resolve(serviceKind, order.getServiceUid(), patient.getInsurancePlanUid(), currency);
            currency = r.currency();
            lines.add(new InvoiceLine(invoice.getUid(), lineKind,
                    order.getServiceUid(), order.getUid(),
                    serviceName + " (" + order.getOrderNo() + ")",
                    BigDecimal.ONE, r.amount(), r.amount()));
            subtotal = subtotal.add(r.amount());
        }

        // 2) Outsider prescriptions that are DISPENSED and not yet billed.
        for (Prescription rx :
                prescriptionRepository.findAllByPatientUidAndConsultationUidIsNullOrderByRequestedAtDesc(patientUid)) {
            if (rx.getStatus() != PrescriptionStatus.SOLD) continue;
            if (alreadyBilled.contains(rx.getUid())) continue;
            Medicine medicine = medicineRepository.findByUid(rx.getMedicineUid()).orElse(null);
            PriceLookup.Resolved r = priceLookup.resolve(ServiceKind.MEDICINE, rx.getMedicineUid(), patient.getInsurancePlanUid(), currency);
            currency = r.currency();
            BigDecimal qty = rx.getQuantity() == null ? BigDecimal.ONE : BigDecimal.valueOf(rx.getQuantity());
            BigDecimal amount = r.amount().multiply(qty);
            String desc = (medicine == null ? rx.getMedicineUid() : medicine.getName())
                    + " · " + rx.getDose() + " · " + rx.getFrequency()
                    + " (" + rx.getPrescriptionNo() + ")";
            lines.add(new InvoiceLine(invoice.getUid(), InvoiceLineKind.MEDICINE,
                    rx.getMedicineUid(), rx.getUid(),
                    desc, qty, r.amount(), amount));
            subtotal = subtotal.add(amount);
        }

        invoiceLineRepository.saveAll(lines);
        invoice.setSubtotal(subtotal);
        invoice.setCurrency(currency);
        return toDto(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDto findCurrentDraftForOutsider(String patientUid) {
        Invoice invoice = invoiceRepository.findDraftOutsiderForPatient(patientUid).orElse(null);
        return invoice == null ? null : toDto(invoice);
    }

    /** Inclusive whole-day count; an admission with no overnight stay is still one day. */
    private static BigDecimal ceilDaysBetween(Instant from, Instant to) {
        long seconds = Math.max(0L, Duration.between(from, to).getSeconds());
        BigDecimal days = BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(86_400L), 0, RoundingMode.CEILING);
        return days.signum() <= 0 ? BigDecimal.ONE : days;
    }

    @Transactional
    public InvoiceDto issue(String uid) {
        Invoice invoice = loadOrThrow(uid);
        invoice.issue();
        return toDto(invoice);
    }

    @Transactional
    public InvoiceDto cancel(String uid, CancelInvoiceRequest request) {
        Invoice invoice = loadOrThrow(uid);
        invoice.cancel(request == null ? null : request.reason());
        return toDto(invoice);
    }

    @Transactional
    public InvoiceDto recordPayment(String uid, RecordPaymentRequest request) {
        Invoice invoice = loadOrThrow(uid);
        if (!invoice.getCurrency().equals(request.currency())) {
            throw new BusinessRuleException("Payment currency " + request.currency()
                    + " does not match invoice currency " + invoice.getCurrency());
        }
        Payment payment = new Payment(
                paymentNumberGenerator.next(),
                invoice.getUid(),
                request.method(),
                request.amount(),
                request.currency(),
                emptyToNull(request.reference()),
                emptyToNull(request.note()));
        paymentRepository.save(payment);
        invoice.applyPayment(request.amount());
        // If this payment fully settled the invoice, push the settled state to
        // the encounter module (consultation-fee gate, dispensed prescriptions).
        settlementDispatcher.onInvoiceMaybeSettled(invoice);
        return toDto(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public InvoiceDto findForConsultation(String consultationUid) {
        Invoice invoice = invoiceRepository.findByConsultationUid(consultationUid).orElse(null);
        return invoice == null ? null : toDto(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDto findForAdmission(String admissionUid) {
        Invoice invoice = invoiceRepository.findByAdmissionUid(admissionUid).orElse(null);
        return invoice == null ? null : toDto(invoice);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummary> search(String query, InvoiceStatus status, String patientUid, Pageable pageable) {
        return PageResponse.from(
                invoiceRepository.search(
                        query == null ? null : query.trim(),
                        status,
                        emptyToNull(patientUid),
                        pageable).map(this::toSummary));
    }

    // ----- mapping helpers ---------------------------------------------------

    private Invoice loadOrThrow(String uid) {
        return invoiceRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + uid));
    }

    private static InvoiceLineKind mapKind(ClinicalOrderKind kind) {
        return switch (kind) {
            case LAB_TEST  -> InvoiceLineKind.LAB_TEST;
            case RADIOLOGY -> InvoiceLineKind.RADIOLOGY;
            case PROCEDURE -> InvoiceLineKind.PROCEDURE;
        };
    }

    private static ServiceKind mapServiceKind(ClinicalOrderKind kind) {
        return switch (kind) {
            case LAB_TEST  -> ServiceKind.LAB_TEST;
            case RADIOLOGY -> ServiceKind.RADIOLOGY;
            case PROCEDURE -> ServiceKind.PROCEDURE;
        };
    }

    private String resolveOrderServiceName(ClinicalOrder o) {
        return switch (o.getKind()) {
            case LAB_TEST  -> labTestTypeRepository.findByUid(o.getServiceUid()).map(LabTestType::getName).orElse(o.getServiceUid());
            case RADIOLOGY -> radiologyTypeRepository.findByUid(o.getServiceUid()).map(RadiologyType::getName).orElse(o.getServiceUid());
            case PROCEDURE -> procedureTypeRepository.findByUid(o.getServiceUid()).map(ProcedureType::getName).orElse(o.getServiceUid());
        };
    }

    private InvoiceDto toDto(Invoice invoice) {
        return dtoAssembler.toDto(invoice);
    }

    private InvoiceSummary toSummary(Invoice i) {
        Patient patient = patientRepository.findByUid(i.getPatientUid()).orElse(null);
        return new InvoiceSummary(
                i.getUid(),
                i.getInvoiceNo(),
                i.getScope(),
                i.getConsultationUid(),
                i.getAdmissionUid(),
                i.getPatientUid(),
                patient == null ? null : patient.fullName(),
                patient == null ? null : patient.getPatientNo(),
                i.getStatus(),
                i.getPaymentType(),
                i.getSubtotal(),
                i.getTotalPaid(),
                i.balance(),
                i.getCurrency(),
                i.getIssuedAt(),
                i.getCreatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
