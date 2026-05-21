package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.encounter.order.application.event.ClinicalOrderRaisedEvent;
import com.otapp.hmis.engine.encounter.prescription.application.event.PrescriptionRaisedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bills consultation orders / prescriptions up front (M13). Fires after-commit
 * on the encounter transaction so the order/Rx is durably persisted; a failure
 * is logged but does not roll back the clinical action (recover via the
 * idempotent recovery endpoints on {@code InvoiceController}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ServiceChargeListeners {

    private final ServiceChargeService serviceChargeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderRaised(ClinicalOrderRaisedEvent event) {
        try {
            serviceChargeService.billOrder(event.orderUid());
        } catch (RuntimeException e) {
            log.error("Failed to bill order {} — recover via POST /billing/orders/uid/{}/charge",
                    event.orderUid(), event.orderUid(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onPrescriptionRaised(PrescriptionRaisedEvent event) {
        try {
            serviceChargeService.billPrescription(event.prescriptionUid());
        } catch (RuntimeException e) {
            log.error("Failed to bill prescription {} — recover via POST /billing/prescriptions/uid/{}/charge",
                    event.prescriptionUid(), event.prescriptionUid(), e);
        }
    }
}
