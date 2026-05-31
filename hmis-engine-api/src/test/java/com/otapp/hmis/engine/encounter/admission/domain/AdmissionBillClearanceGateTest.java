package com.otapp.hmis.engine.encounter.admission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage of the legacy-faithful admission bill-clearance gate
 * (PatientResource.get_discharge_summary / get_referral_summary /
 * get_deceased_summary): closure (discharge / deceased / referral-out) is blocked
 * while the admission still has an outstanding bill, signalled by the local
 * {@code billsCleared} flag that the billing settlement dispatcher maintains.
 * No Spring, no DB.
 */
class AdmissionBillClearanceGateTest {

    private static Admission admission() {
        return new Admission("AD-1", "PT00000000000000000000001", "WARD000000000000000000001",
                "B-1", "dr.house", PaymentType.CASH, null, null, "obs");
    }

    @Test
    void newAdmissionStartsCleared() {
        // No bills yet => trivially clear, matching legacy where an admission with
        // no UNPAID/VERIFIED PatientBill is dischargeable.
        assertThat(admission().isBillsCleared()).isTrue();
        assertThat(admission().getBillsClearedAt()).isNull();
    }

    @Test
    void dischargeBlockedWhileBillsUncleared() {
        Admission a = admission();
        a.clearBillsClearedFlag();        // billing: outstanding admission invoice issued
        assertThat(a.isBillsCleared()).isFalse();
        assertThatThrownBy(() -> a.discharge("done"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Patient has uncleared bills");
        assertThat(a.getStatus()).isEqualTo(AdmissionStatus.ADMITTED); // unchanged
    }

    @Test
    void deceasedAndTransferOutAlsoBlockedWhileUncleared() {
        Admission a = admission();
        a.clearBillsClearedFlag();
        assertThatThrownBy(() -> a.markDeceased("rip")).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Patient has uncleared bills");
        assertThatThrownBy(() -> a.transferOut("ref")).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Patient has uncleared bills");
        assertThat(a.getStatus()).isEqualTo(AdmissionStatus.ADMITTED);
    }

    @Test
    void dischargeAllowedOnceBillsCleared() {
        Admission a = admission();
        a.clearBillsClearedFlag();        // outstanding
        a.markBillsCleared();             // billing: invoice fully settled (PAID)
        assertThat(a.isBillsCleared()).isTrue();
        assertThat(a.getBillsClearedAt()).isNotNull();
        assertThatCode(() -> a.discharge("done")).doesNotThrowAnyException();
        assertThat(a.getStatus()).isEqualTo(AdmissionStatus.DISCHARGED);
    }

    @Test
    void markBillsClearedIsIdempotent() {
        Admission a = admission();
        a.clearBillsClearedFlag();
        a.markBillsCleared();
        var firstStamp = a.getBillsClearedAt();
        a.markBillsCleared();             // second call must not move the stamp
        assertThat(a.getBillsClearedAt()).isEqualTo(firstStamp);
    }

    @Test
    void clearBillsClearedFlagIsIdempotentAndResetsStamp() {
        Admission a = admission();        // starts cleared with no stamp
        a.clearBillsClearedFlag();
        assertThat(a.isBillsCleared()).isFalse();
        assertThat(a.getBillsClearedAt()).isNull();
        a.clearBillsClearedFlag();        // idempotent
        assertThat(a.isBillsCleared()).isFalse();
    }

    @Test
    void refundReopensGateAfterSettlement() {
        Admission a = admission();
        a.clearBillsClearedFlag();        // outstanding
        a.markBillsCleared();             // paid -> dischargeable
        a.clearBillsClearedFlag();        // refund re-opens balance -> billing re-arms gate
        assertThatThrownBy(() -> a.discharge("done")).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Patient has uncleared bills");
    }

    @Test
    void cancelIsNotGatedOnBills() {
        // Legacy gate fires only on SIGNED-OUT (discharge / referral / deceased),
        // not on entered-in-error cancellation.
        Admission a = admission();
        a.clearBillsClearedFlag();
        assertThatCode(() -> a.cancel("entered in error")).doesNotThrowAnyException();
        assertThat(a.getStatus()).isEqualTo(AdmissionStatus.CANCELLED);
    }
}
