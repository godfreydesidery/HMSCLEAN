package com.otapp.hmis.engine.encounter.consultation.application;

import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encounter-internal sign-out cascade (legacy {@code free_consultation} step 5):
 * when a consultation is signed out, every downstream lab / radiology / procedure
 * order and prescription that has NOT been settled (paid) and is still in a
 * non-terminal state is cancelled. Settled (paid) and already-terminal items —
 * COMPLETED / SOLD / CANCELLED / REJECTED — are left intact, mirroring the legacy
 * "only UNPAID downstream is cancelled; PAID/COVERED bills are left intact".
 *
 * <p>This keeps all order / prescription mutation inside the encounter module —
 * the billing-side reversal (voiding the unpaid invoice lines) runs separately
 * via {@code ConsultationSignedOutEvent}. The encounter module never reaches
 * into billing.
 */
@Service
@RequiredArgsConstructor
public class ConsultationCloseService {

    private static final String SIGNED_OUT_REASON = "Consultation signed out";

    private final ClinicalOrderRepository orderRepository;
    private final PrescriptionRepository prescriptionRepository;

    /**
     * Cancel every unsettled, non-terminal downstream order / prescription of a
     * consultation. Idempotent — already-cancelled / settled rows are skipped.
     */
    @Transactional
    public void cancelUnsettledDownstream(String consultationUid) {
        List<ClinicalOrder> orders = orderRepository.findAllByConsultationUidOrderByRequestedAtDesc(consultationUid);
        for (ClinicalOrder order : orders) {
            if (order.isSettled()) continue;                                   // legacy: PAID left intact
            if (isOrderTerminal(order.getStatus())) continue;                  // COMPLETED/CANCELLED — nothing to void
            order.cancel(SIGNED_OUT_REASON);
        }

        List<Prescription> prescriptions =
                prescriptionRepository.findAllByConsultationUidOrderByRequestedAtDesc(consultationUid);
        for (Prescription rx : prescriptions) {
            if (rx.isSettled()) continue;                                      // legacy: PAID left intact
            if (isPrescriptionTerminal(rx.getStatus())) continue;              // SOLD/CANCELLED/REJECTED — leave as-is
            rx.cancel(SIGNED_OUT_REASON);
        }
    }

    private static boolean isOrderTerminal(ClinicalOrderStatus status) {
        return status == ClinicalOrderStatus.COMPLETED || status == ClinicalOrderStatus.CANCELLED;
    }

    private static boolean isPrescriptionTerminal(PrescriptionStatus status) {
        return status == PrescriptionStatus.SOLD
                || status == PrescriptionStatus.CANCELLED
                || status == PrescriptionStatus.REJECTED;
    }
}
