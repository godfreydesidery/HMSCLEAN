package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.encounter.consultation.application.event.ConsultationBookedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Seeds the consultation-fee invoice once a consultation is booked — the legacy
 * "send to doctor creates the consultation bill" step. Fires after-commit so the
 * invoice is seeded against a durably-persisted consultation; failure is logged
 * but does not roll back the booking (recover via
 * {@code POST /billing/consultations/uid/{uid}/consultation-fee}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ConsultationFeeListeners {

    private final ConsultationFeeService consultationFeeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onConsultationBooked(ConsultationBookedEvent event) {
        try {
            consultationFeeService.ensureFor(event.consultationUid());
        } catch (RuntimeException e) {
            log.error("Failed to seed consultation invoice for {} — recover via "
                    + "POST /billing/consultations/uid/{}/consultation-fee",
                    event.consultationUid(), event.consultationUid(), e);
        }
    }
}
