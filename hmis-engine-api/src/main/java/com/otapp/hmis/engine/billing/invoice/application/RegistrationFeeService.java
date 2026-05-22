package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceDto;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.billing.invoice.infrastructure.InvoiceNumberGenerator;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyService;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time registration fee per patient. Created automatically when a
 * patient is registered (via {@code PatientRegisteredEvent}); the public
 * endpoint exists for idempotent recovery if the after-commit listener
 * fails for any reason. Pricing comes from the {@link ServiceKind#REGISTRATION}
 * row in the service-price matrix with sentinel {@code serviceUid = "DEFAULT"};
 * insurance plans can override or waive (amount = 0).
 */
@Service
@RequiredArgsConstructor
public class RegistrationFeeService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final PatientRepository patientRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final PriceLookup priceLookup;
    private final InvoiceDtoAssembler invoiceDtoAssembler;
    private final CurrencyService currencyService;

    /**
     * Idempotent — returns the existing registration invoice if one already
     * exists for the patient, otherwise creates it (DRAFT → ISSUED). The line
     * carries the resolved registration price; the invoice subtotal is set
     * accordingly. Plan waivers manifest as a zero-amount line; the gate
     * query ignores zero-balance invoices.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InvoiceDto ensureFor(String patientUid) {
        Patient patient = patientRepository.findByUid(patientUid)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientUid));

        Invoice existing = invoiceRepository.findRegistrationForPatient(patientUid).orElse(null);
        if (existing != null) {
            return invoiceDtoAssembler.toDto(existing);
        }

        // REQUIRES_NEW above is mandatory — the after-commit listener path otherwise leaves
        // Hibernate without a committable EntityManager and the save silently never reaches the DB.
        String defaultCurrency = currencyService.defaultCode();
        Invoice invoice = Invoice.forRegistration(
                invoiceNumberGenerator.next(),
                patient.getUid(),
                patient.getPaymentType(),
                patient.getInsurancePlanUid(),
                defaultCurrency);
        invoiceRepository.save(invoice);

        PriceLookup.Resolved r = priceLookup.resolve(
                ServiceKind.REGISTRATION, ServiceKind.REGISTRATION_SERVICE_UID,
                patient.getInsurancePlanUid(), defaultCurrency);
        BigDecimal amount = r.amount();
        invoice.setCurrency(r.currency());
        invoice.setSubtotal(amount);

        InvoiceLine line = new InvoiceLine(
                invoice.getUid(), InvoiceLineKind.REGISTRATION,
                ServiceKind.REGISTRATION_SERVICE_UID, patient.getUid(),
                "Patient registration fee — " + patient.getPatientNo(),
                BigDecimal.ONE, amount, amount);
        invoiceLineRepository.save(line);

        invoice.issue();
        return invoiceDtoAssembler.toDto(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDto findFor(String patientUid) {
        Invoice invoice = invoiceRepository.findRegistrationForPatient(patientUid).orElse(null);
        return invoice == null ? null : invoiceDtoAssembler.toDto(invoice);
    }
}
