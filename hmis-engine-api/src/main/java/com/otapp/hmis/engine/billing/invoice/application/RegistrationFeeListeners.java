package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.encounter.consultation.application.event.ConsultationBookingRequestedEvent;
import com.otapp.hmis.engine.patient.application.event.PatientRegisteredEvent;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Billing-side event listeners that react to the patient + encounter
 * lifecycles without forcing those modules to depend on billing.
 *
 * <p>{@code PatientRegisteredEvent} fires after-commit on the patient
 * transaction so the registration invoice is seeded once the patient is
 * durably persisted. Failure to seed is logged but does not roll back the
 * registration — the idempotent {@code POST /billing/patients/uid/{uid}
 * /registration-fee} endpoint exists for recovery.
 *
 * <p>{@code ConsultationBookingRequestedEvent} fires synchronously inside
 * the consultation transaction, so a thrown {@link BusinessRuleException}
 * aborts the booking. CASH patients with an outstanding registration
 * balance are refused.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class RegistrationFeeListeners {

    private final RegistrationFeeService registrationFeeService;
    private final InvoiceRepository invoiceRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onPatientRegistered(PatientRegisteredEvent event) {
        try {
            registrationFeeService.ensureFor(event.patientUid());
        } catch (RuntimeException e) {
            log.error("Failed to seed registration invoice for patient {} — recover via POST /billing/patients/uid/{}/registration-fee",
                    event.patientUid(), event.patientUid(), e);
        }
    }

    @EventListener
    void onConsultationBookingRequested(ConsultationBookingRequestedEvent event) {
        if (event.paymentType() != PaymentType.CASH) {
            return; // only CASH patients are gated on the registration fee
        }
        if (invoiceRepository.hasUnpaidRegistration(event.patientUid())) {
            throw new BusinessRuleException(
                    "Patient has an unpaid registration fee — settle it at the cashier before booking a consultation");
        }
    }
}
