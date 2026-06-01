package com.otapp.hmis.engine.encounter.order.application;

import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionService;
import com.otapp.hmis.engine.patient.application.event.PatientLeftOutsiderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Encounter-side reaction to a walk-in being converted to a registered patient
 * (REG-2): cancel the patient's now-orphaned open OUTSIDER orders and
 * prescriptions. Fires after-commit on the patient transaction; failures are
 * logged but do not roll back the type change (the billing-side invoice discard
 * runs independently off the same event).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class OutsiderSweepListeners {

    private static final String REASON = "Patient converted from walk-in (no longer an outsider)";

    private final ClinicalOrderService orderService;
    private final PrescriptionService prescriptionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onPatientLeftOutsider(PatientLeftOutsiderEvent event) {
        try {
            int orders = orderService.cancelOpenOutsiderForPatient(event.patientUid(), REASON);
            int scripts = prescriptionService.cancelOpenOutsiderForPatient(event.patientUid(), REASON);
            if (orders + scripts > 0) {
                log.info("Swept {} open outsider order(s) and {} prescription(s) for converted patient {}",
                        orders, scripts, event.patientUid());
            }
        } catch (RuntimeException e) {
            log.error("Failed to sweep outsider orders/prescriptions for patient {}", event.patientUid(), e);
        }
    }
}
