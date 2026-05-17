package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.CancelInvoiceRequest;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceDto;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceLineDto;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceSummary;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.PaymentDto;
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
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestType;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestTypeRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureType;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureTypeRepository;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyType;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyTypeRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ClinicRepository clinicRepository;
    private final LabTestTypeRepository labTestTypeRepository;
    private final RadiologyTypeRepository radiologyTypeRepository;
    private final ProcedureTypeRepository procedureTypeRepository;
    private final MedicineRepository medicineRepository;
    private final PatientRepository patientRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final PaymentNumberGenerator paymentNumberGenerator;
    private final PriceLookup priceLookup;

    /**
     * Generates or regenerates the invoice for a consultation. Only allowed
     * while the existing invoice is still DRAFT; once issued, the lines are
     * locked. Returns the (possibly fresh) invoice with all its lines.
     */
    @Transactional
    public InvoiceDto generateForConsultation(String consultationUid) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));

        Invoice invoice = invoiceRepository.findByConsultationUid(consultationUid).orElse(null);
        if (invoice != null && invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Invoice is already " + invoice.getStatus() + " and cannot be regenerated");
        }
        if (invoice == null) {
            invoice = new Invoice(
                    invoiceNumberGenerator.next(),
                    consultation.getUid(),
                    consultation.getPatientUid(),
                    consultation.getPaymentType(),
                    consultation.getInsurancePlanUid(),
                    DEFAULT_CURRENCY);
            invoiceRepository.save(invoice);
        } else {
            invoiceLineRepository.deleteAllByInvoiceUid(invoice.getUid());
            invoice.setSubtotal(BigDecimal.ZERO);
            invoice.setPaymentType(consultation.getPaymentType());
            invoice.setInsurancePlanUid(consultation.getInsurancePlanUid());
        }

        List<InvoiceLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = invoice.getCurrency();

        // 1) Consultation fee
        Clinic clinic = clinicRepository.findByUid(consultation.getClinicUid()).orElse(null);
        if (clinic != null) {
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
            if (rx.getStatus() != PrescriptionStatus.DISPENSED) continue;
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
        List<InvoiceLine> lines = invoiceLineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(invoice.getUid());
        List<Payment> payments = paymentRepository.findAllByInvoiceUidOrderByReceivedAtAsc(invoice.getUid());
        Patient patient = patientRepository.findByUid(invoice.getPatientUid()).orElse(null);
        InsurancePlan plan = invoice.getInsurancePlanUid() == null
                ? null
                : insurancePlanRepository.findByUid(invoice.getInsurancePlanUid()).orElse(null);

        return new InvoiceDto(
                invoice.getUid(),
                invoice.getInvoiceNo(),
                invoice.getConsultationUid(),
                invoice.getPatientUid(),
                patient == null ? null : patient.fullName(),
                patient == null ? null : patient.getPatientNo(),
                invoice.getPaymentType(),
                invoice.getInsurancePlanUid(),
                plan == null ? null : plan.getName(),
                invoice.getCurrency(),
                invoice.getSubtotal(),
                invoice.getTotalPaid(),
                invoice.balance(),
                invoice.getStatus(),
                invoice.getIssuedAt(),
                invoice.getPaidAt(),
                invoice.getCancelledAt(),
                invoice.getCancelReason(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                lines.stream().map(InvoiceService::toLineDto).toList(),
                payments.stream().map(InvoiceService::toPaymentDto).toList());
    }

    private InvoiceSummary toSummary(Invoice i) {
        Patient patient = patientRepository.findByUid(i.getPatientUid()).orElse(null);
        return new InvoiceSummary(
                i.getUid(),
                i.getInvoiceNo(),
                i.getConsultationUid(),
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

    private static InvoiceLineDto toLineDto(InvoiceLine l) {
        return new InvoiceLineDto(
                l.getUid(),
                l.getKind(),
                l.getServiceUid(),
                l.getReferenceUid(),
                l.getDescription(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getAmount());
    }

    private static PaymentDto toPaymentDto(Payment p) {
        return new PaymentDto(
                p.getUid(),
                p.getPaymentNo(),
                p.getMethod(),
                p.getAmount(),
                p.getCurrency(),
                p.getReference(),
                p.getNote(),
                p.getReceivedAt(),
                p.getCreatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
