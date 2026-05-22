package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceDto;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.billing.invoice.infrastructure.InvoiceNumberGenerator;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationService;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyService;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The consultation fee, billed up front when the patient is sent to a doctor
 * (legacy "send to doctor creates the consultation bill"). Created
 * automatically from {@code ConsultationBookedEvent}; the public endpoint
 * exists for idempotent recovery if the after-commit listener fails.
 *
 * <p>Pricing comes from {@link ServiceKind#CONSULTATION} for the clinic, with
 * insurance-plan overrides. Follow-up visits are waived (zero amount) — the
 * legacy "bill status NONE". A zero-amount invoice settles the consultation fee
 * immediately so the doctor can open it without a cashier step.
 */
@Service
@RequiredArgsConstructor
public class ConsultationFeeService {

    private final InvoiceRepository invoiceRepository;
    private final com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository invoiceLineRepository;
    private final ConsultationRepository consultationRepository;
    private final ClinicRepository clinicRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final PriceLookup priceLookup;
    private final InvoiceDtoAssembler invoiceDtoAssembler;
    private final ConsultationService consultationService;
    private final CurrencyService currencyService;

    /**
     * Idempotent — returns the existing consultation invoice if one exists,
     * otherwise creates it (DRAFT → ISSUED) with a single CONSULTATION fee
     * line. Settles the fee immediately when the amount is zero (follow-up /
     * plan waiver). REQUIRES_NEW because it runs from an after-commit listener.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InvoiceDto ensureFor(String consultationUid) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));

        Invoice existing = invoiceRepository.findByConsultationUid(consultationUid).orElse(null);
        if (existing != null) {
            return invoiceDtoAssembler.toDto(existing);
        }

        boolean followUp = consultation.getFollowUpOfConsultationUid() != null;
        String defaultCurrency = currencyService.defaultCode();

        Invoice invoice = Invoice.forConsultation(
                invoiceNumberGenerator.next(),
                consultation.getUid(),
                consultation.getPatientUid(),
                consultation.getPaymentType(),
                consultation.getInsurancePlanUid(),
                defaultCurrency);
        invoiceRepository.save(invoice);

        Clinic clinic = clinicRepository.findByUid(consultation.getClinicUid()).orElse(null);
        BigDecimal amount = BigDecimal.ZERO;
        String currency = defaultCurrency;
        String clinicName = clinic == null ? consultation.getClinicUid() : clinic.getName();
        if (clinic != null && !followUp) {
            PriceLookup.Resolved r = priceLookup.resolve(
                    ServiceKind.CONSULTATION, clinic.getUid(),
                    consultation.getInsurancePlanUid(), defaultCurrency);
            amount = r.amount();
            currency = r.currency();
        }
        invoice.setCurrency(currency);
        invoice.setSubtotal(amount);

        InvoiceLine line = new InvoiceLine(
                invoice.getUid(), InvoiceLineKind.CONSULTATION,
                consultation.getClinicUid(), consultation.getUid(),
                (followUp ? "Consultation (follow-up) — " : "Consultation — ") + clinicName,
                BigDecimal.ONE, amount, amount);
        invoiceLineRepository.save(line);

        invoice.issue();

        // Zero-amount (follow-up / plan waiver) settles the fee immediately so
        // the doctor can open without a cashier step. Otherwise the fee settles
        // when the cashier records payment (SettlementDispatcher).
        if (amount.signum() == 0) {
            consultationService.markFeeSettled(consultation.getUid());
        }
        return invoiceDtoAssembler.toDto(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDto findFor(String consultationUid) {
        Invoice invoice = invoiceRepository.findByConsultationUid(consultationUid).orElse(null);
        return invoice == null ? null : invoiceDtoAssembler.toDto(invoice);
    }
}
