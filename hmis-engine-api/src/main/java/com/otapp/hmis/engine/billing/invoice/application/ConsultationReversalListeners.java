package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.encounter.consultation.application.event.ConsultationCancelledEvent;
import com.otapp.hmis.engine.encounter.consultation.application.event.ConsultationSignedOutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Billing-side cascade for the legacy consultation cancel / sign-out. Listens
 * after-commit so the reversal runs against durably-persisted encounter state;
 * failures are logged but do not roll back the encounter close (recover via the
 * billing reversal endpoints). Direction is billing → encounter, which the
 * modulith allows; the encounter module never imports billing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ConsultationReversalListeners {

    private final ConsultationReversalService reversalService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onConsultationCancelled(ConsultationCancelledEvent event) {
        try {
            reversalService.reverseConsultationFee(event.consultationUid());
        } catch (RuntimeException e) {
            log.error("Failed to reverse the consultation-fee invoice for {} — "
                    + "recover via the billing reversal endpoint", event.consultationUid(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onConsultationSignedOut(ConsultationSignedOutEvent event) {
        try {
            reversalService.voidUnpaidDownstreamLines(event.consultationUid());
        } catch (RuntimeException e) {
            log.error("Failed to void unpaid downstream lines for signed-out consultation {} — "
                    + "recover by regenerating the consultation invoice", event.consultationUid(), e);
        }
    }
}
