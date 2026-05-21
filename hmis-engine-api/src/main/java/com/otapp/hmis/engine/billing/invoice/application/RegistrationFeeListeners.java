package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.patient.application.event.PatientRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Billing-side reaction to patient registration: seed the one-time
 * registration invoice without forcing the patient module to depend on billing.
 *
 * <p>{@code PatientRegisteredEvent} fires after-commit on the patient
 * transaction so the registration invoice is seeded once the patient is
 * durably persisted. Failure to seed is logged but does not roll back the
 * registration — the idempotent {@code POST /billing/patients/uid/{uid}
 * /registration-fee} endpoint exists for recovery.
 *
 * <p>The registration fee no longer gates consultation booking: legacy-faithfully,
 * the registration bill is simply collected at the cashier, and the gate that
 * blocks a doctor from opening a consultation is the <em>consultation</em> fee
 * (see {@code ConsultationFeeService} + {@code Consultation.feeSettled}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class RegistrationFeeListeners {

    private final RegistrationFeeService registrationFeeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onPatientRegistered(PatientRegisteredEvent event) {
        try {
            registrationFeeService.ensureFor(event.patientUid());
        } catch (RuntimeException e) {
            log.error("Failed to seed registration invoice for patient {} — recover via POST /billing/patients/uid/{}/registration-fee",
                    event.patientUid(), event.patientUid(), e);
        }
    }
}
