package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.patient.application.event.PatientLeftOutsiderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Billing-side reaction to a walk-in being converted to a registered patient
 * (REG-2): discard the patient's now-orphaned draft OUTSIDER invoice, without
 * forcing the patient module to depend on billing. Fires after-commit; failure
 * is logged but does not roll back the type change.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class OutsiderInvoiceSweepListener {

    private final InvoiceService invoiceService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onPatientLeftOutsider(PatientLeftOutsiderEvent event) {
        try {
            invoiceService.discardDraftOutsiderForPatient(event.patientUid());
        } catch (RuntimeException e) {
            log.error("Failed to discard draft outsider invoice for patient {}", event.patientUid(), e);
        }
    }
}
