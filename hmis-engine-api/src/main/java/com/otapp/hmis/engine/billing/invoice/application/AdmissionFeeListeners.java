package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.encounter.admission.application.event.AdmissionAdmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Seeds + issues the admission (ward-bed) invoice once a patient is admitted —
 * the legacy "doAdmission creates the ward-bed bill" step. Fires after-commit so
 * the invoice is seeded against a durably-persisted admission; issuing it arms the
 * discharge bill-clearance gate, so a patient can no longer be discharged having
 * paid nothing. Failure is logged but does not roll back the admission (recover
 * via {@code POST /billing/admissions/uid/{uid}/invoice} then issue).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AdmissionFeeListeners {

    private final InvoiceService invoiceService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onAdmitted(AdmissionAdmittedEvent event) {
        try {
            invoiceService.seedAdmissionInvoice(event.admissionUid());
        } catch (RuntimeException e) {
            log.error("Failed to seed admission invoice for {} — recover via "
                    + "POST /billing/admissions/uid/{}/invoice", event.admissionUid(), event.admissionUid(), e);
        }
    }
}
