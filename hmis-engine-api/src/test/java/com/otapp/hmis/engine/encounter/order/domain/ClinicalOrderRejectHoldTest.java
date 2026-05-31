package com.otapp.hmis.engine.encounter.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage of the legacy-faithful reject/hold/re-accept lifecycle on
 * lab/radiology clinical orders (no Spring, no DB).
 */
class ClinicalOrderRejectHoldTest {

    private static ClinicalOrder lab() {
        return new ClinicalOrder("ORD-1", "CONS1", "PT1", ClinicalOrderKind.LAB_TEST, "SVC1", OrderUrgency.NORMAL, null);
    }

    private static ClinicalOrder procedure() {
        return new ClinicalOrder("ORD-2", "CONS1", "PT1", ClinicalOrderKind.PROCEDURE, "SVC2", OrderUrgency.NORMAL, null);
    }

    @Test
    void rejectFromRequestedCapturesReasonAndClearsAcceptStamp() {
        ClinicalOrder o = lab();
        o.accept();                       // REQUESTED -> ACCEPTED (sets acceptedAt)
        o.reject("Specimen haemolysed", "tech1");
        assertThat(o.getStatus()).isEqualTo(ClinicalOrderStatus.REJECTED);
        assertThat(o.getRejectReason()).isEqualTo("Specimen haemolysed");
        assertThat(o.getRejectedByUsername()).isEqualTo("tech1");
        assertThat(o.getRejectedAt()).isNotNull();
        assertThat(o.getAcceptedAt()).isNull();
    }

    @Test
    void reAcceptFromRejectedClearsRejectionAudit() {
        ClinicalOrder o = lab();
        o.reject("wrong tube", "tech1");
        o.accept();                       // re-accept loop
        assertThat(o.getStatus()).isEqualTo(ClinicalOrderStatus.ACCEPTED);
        assertThat(o.getRejectReason()).isNull();
        assertThat(o.getRejectedAt()).isNull();
        assertThat(o.getRejectedByUsername()).isNull();
        assertThat(o.getAcceptedAt()).isNotNull();
    }

    @Test
    void holdReturnsAcceptedOrderToRequestedAndStampsHolder() {
        ClinicalOrder o = lab();
        o.accept();
        o.hold("tech2");
        assertThat(o.getStatus()).isEqualTo(ClinicalOrderStatus.REQUESTED); // legacy: bounce to pending
        assertThat(o.getHeldAt()).isNotNull();
        assertThat(o.getHeldByUsername()).isEqualTo("tech2");
        assertThat(o.getAcceptedAt()).isNull();
    }

    @Test
    void holdOnlyValidFromAccepted() {
        assertThatThrownBy(() -> lab().hold("u")).isInstanceOf(BusinessRuleException.class); // from REQUESTED
    }

    @Test
    void rejectOnlyFromRequestedOrAccepted() {
        ClinicalOrder o = lab();
        o.accept();
        o.markInProgress();               // ACCEPTED -> IN_PROGRESS
        assertThatThrownBy(() -> o.reject("late", "u")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void acceptRejectedThenStillCannotAcceptCompleted() {
        ClinicalOrder o = lab();
        o.accept();
        o.markInProgress();
        assertThatThrownBy(o::accept).isInstanceOf(BusinessRuleException.class); // not REQUESTED/REJECTED
    }

    @Test
    void proceduresCannotBeRejectedOrHeld() {
        assertThatThrownBy(() -> procedure().reject("x", "u")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> procedure().hold("u")).isInstanceOf(BusinessRuleException.class);
    }
}
