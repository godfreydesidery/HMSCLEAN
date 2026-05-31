package com.otapp.hmis.engine.encounter.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationCloseService;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderStatus;
import com.otapp.hmis.engine.encounter.order.domain.OrderUrgency;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit coverage of the consultation-gates gap (no Spring, no DB):
 *  - the IN_PROGRESS clinical-authoring gate (legacy open_consultation confinement)
 *  - the sign-out downstream cascade (legacy free_consultation step 5):
 *    UNPAID/unsettled non-terminal orders + Rx are cancelled; settled and
 *    already-terminal items are left intact.
 */
@ExtendWith(MockitoExtension.class)
class ConsultationGatesTest {

    private static final String CONS = "01HCONSULT0000000000000AAA";

    @Mock private ClinicalOrderRepository orderRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @InjectMocks private ConsultationCloseService closeService;

    private static Consultation consultation() {
        return new Consultation("CN-1", "PT1", "CL1", "drhouse", PaymentType.CASH, null, null);
    }

    private static ClinicalOrder order(ClinicalOrderStatus status, boolean settled) {
        ClinicalOrder o = new ClinicalOrder("ORD", CONS, "PT1",
                ClinicalOrderKind.LAB_TEST, "SVC", OrderUrgency.NORMAL, null);
        ReflectionTestUtils.setField(o, "status", status);
        if (settled) o.markSettled();
        return o;
    }

    private static Prescription rx(PrescriptionStatus status, boolean settled) {
        Prescription p = new Prescription("RX", CONS, "PT1", "MED", "1 tab", "BD", 5, 10, null);
        ReflectionTestUtils.setField(p, "status", status);
        if (settled) p.markSettled();
        return p;
    }

    // ----- IN_PROGRESS authoring gate ---------------------------------------

    @Test
    void requireAuthorableThrowsUnlessInProgress() {
        Consultation booked = consultation();                       // BOOKED on construction
        assertThatThrownBy(booked::requireAuthorable)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("IN_PROGRESS");

        booked.start();                                             // BOOKED -> IN_PROGRESS
        assertThatCode(booked::requireAuthorable).doesNotThrowAnyException();

        booked.complete();                                          // IN_PROGRESS -> COMPLETED
        assertThatThrownBy(booked::requireAuthorable)
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void completeStampsSignedOutAt() {
        Consultation c = consultation();
        c.start();
        c.complete();
        assertThat(c.getSignedOutAt()).isNotNull();
    }

    // ----- sign-out downstream cascade --------------------------------------

    @Test
    void signOutCancelsUnsettledNonTerminalDownstreamOnly() {
        ClinicalOrder unpaidRequested = order(ClinicalOrderStatus.REQUESTED, false); // cancelled
        ClinicalOrder paidAccepted    = order(ClinicalOrderStatus.ACCEPTED, true);   // intact (settled)
        ClinicalOrder completed       = order(ClinicalOrderStatus.COMPLETED, false); // intact (terminal)
        Prescription unpaidPending    = rx(PrescriptionStatus.PENDING, false);       // cancelled
        Prescription soldRx           = rx(PrescriptionStatus.SOLD, false);          // intact (terminal)
        Prescription paidPending      = rx(PrescriptionStatus.PENDING, true);        // intact (settled)

        when(orderRepository.findAllByConsultationUidOrderByRequestedAtDesc(CONS))
                .thenReturn(List.of(unpaidRequested, paidAccepted, completed));
        when(prescriptionRepository.findAllByConsultationUidOrderByRequestedAtDesc(CONS))
                .thenReturn(List.of(unpaidPending, soldRx, paidPending));

        closeService.cancelUnsettledDownstream(CONS);

        assertThat(unpaidRequested.getStatus()).isEqualTo(ClinicalOrderStatus.CANCELLED);
        assertThat(unpaidRequested.getCancelReason()).isEqualTo("Consultation signed out");
        assertThat(paidAccepted.getStatus()).isEqualTo(ClinicalOrderStatus.ACCEPTED);
        assertThat(completed.getStatus()).isEqualTo(ClinicalOrderStatus.COMPLETED);

        assertThat(unpaidPending.getStatus()).isEqualTo(PrescriptionStatus.CANCELLED);
        assertThat(soldRx.getStatus()).isEqualTo(PrescriptionStatus.SOLD);
        assertThat(paidPending.getStatus()).isEqualTo(PrescriptionStatus.PENDING);
    }
}
