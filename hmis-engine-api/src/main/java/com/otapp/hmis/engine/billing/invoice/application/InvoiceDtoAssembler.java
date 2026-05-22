package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceDto;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceLineDto;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.PaymentDto;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.payment.domain.Payment;
import com.otapp.hmis.engine.billing.payment.domain.PaymentRepository;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds {@link InvoiceDto} responses, loading the patient / plan / lines /
 * payments related to an invoice. Shared by {@link InvoiceService} and
 * {@link RegistrationFeeService} so both return identically-shaped responses.
 */
@Component
@RequiredArgsConstructor
class InvoiceDtoAssembler {

    private final InvoiceLineRepository invoiceLineRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final InvoiceLinePricing linePricing;

    InvoiceDto toDto(Invoice invoice) {
        List<InvoiceLine> lines = invoiceLineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(invoice.getUid());
        List<Payment> payments = paymentRepository.findAllByInvoiceUidOrderByReceivedAtAsc(invoice.getUid());
        Patient patient = patientRepository.findByUid(invoice.getPatientUid()).orElse(null);
        InsurancePlan plan = invoice.getInsurancePlanUid() == null
                ? null
                : insurancePlanRepository.findByUid(invoice.getInsurancePlanUid()).orElse(null);
        boolean overridable = linePricing.overridable(invoice);

        return new InvoiceDto(
                invoice.getUid(),
                invoice.getInvoiceNo(),
                invoice.getScope(),
                invoice.getConsultationUid(),
                invoice.getAdmissionUid(),
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
                lines.stream().map(l -> toLineDto(l, invoice.getInsurancePlanUid(), invoice.getCurrency(), overridable)).toList(),
                payments.stream().map(InvoiceDtoAssembler::toPaymentDto).toList());
    }

    private InvoiceLineDto toLineDto(InvoiceLine l, String planUid, String currency, boolean overridable) {
        PriceLookup.Resolved band = linePricing.bandFor(l, planUid, currency);
        return new InvoiceLineDto(
                l.getUid(),
                l.getKind(),
                l.getServiceUid(),
                l.getReferenceUid(),
                l.getDescription(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getAmount(),
                band == null ? null : band.minAmount(),
                band == null ? null : band.maxAmount(),
                overridable);
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
}
