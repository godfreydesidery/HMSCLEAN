package com.otapp.hmis.engine.billing.invoice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.otapp.hmis.engine.billing.invoice.application.CoverageResolver.CoverageResolution;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePrice;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePriceRepository;
import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PatientType;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit coverage of the charge-time payer routing DECISION (no Spring, no DB).
 * Faithful to legacy Zana-HMIS {@code PatientServiceImpl} accrual: coverage is
 * keyed off the per-service {@code ServicePrice.covered} flag (NOT
 * price-existence), the membership number is stamped on a covered line, and the
 * cash remainder above the plan ceiling becomes a (never-negative) co-pay.
 */
@ExtendWith(MockitoExtension.class)
class CoverageResolverTest {

    private static final String PLAN = "01PLAN0000000000000000000A";
    private static final String LAB = "01LAB00000000000000000000A";
    private static final String PATIENT = "01PT000000000000000000000A";
    private static final String CCY = "TZS";

    @Mock private ServicePriceRepository priceRepository;
    @Mock private PatientRepository patientRepository;
    @InjectMocks private CoverageResolver resolver;

    private static ServicePrice coveredPlanCell(BigDecimal amount) {
        ServicePrice p = new ServicePrice(PLAN, ServiceKind.LAB_TEST, LAB, amount, CCY, null);
        p.setCovered(true);
        return p;
    }

    private Patient insuredPatient() {
        Patient p = new Patient("PT-1", "Asha", null, "Mussa",
                LocalDate.of(1990, 1, 1), Gender.FEMALE, PatientType.OUTPATIENT, PaymentType.INSURANCE);
        p.setInsurancePlanUid(PLAN);
        p.setMembershipNo("MEM-123");
        return p;
    }

    @Test
    void patientWithNoPlanIsNotCovered() {
        Patient p = insuredPatient();
        p.setInsurancePlanUid(null);
        when(patientRepository.findByUid(PATIENT)).thenReturn(Optional.of(p));

        CoverageResolution r = resolver.resolve(ServiceKind.LAB_TEST, LAB, PATIENT, CCY, new BigDecimal("100.00"));

        assertThat(r.covered()).isFalse();
        assertThat(r.coveredAmount()).isEqualByComparingTo("100.00"); // cash baseline echoed
        assertThat(r.planUid()).isNull();
        assertThat(r.membershipNo()).isNull();
    }

    @Test
    void insuredButPlanDoesNotCoverServiceIsNotCovered() {
        when(patientRepository.findByUid(PATIENT)).thenReturn(Optional.of(insuredPatient()));
        when(priceRepository.findCoveredCell(PLAN, ServiceKind.LAB_TEST, LAB, CCY)).thenReturn(Optional.empty());

        CoverageResolution r = resolver.resolve(ServiceKind.LAB_TEST, LAB, PATIENT, CCY, new BigDecimal("100.00"));

        assertThat(r.covered()).isFalse();
        assertThat(r.coveredAmount()).isEqualByComparingTo("100.00");
        assertThat(r.copayAmount()).isEqualByComparingTo("0");
    }

    @Test
    void coveredServiceRoutesToInsurerWithMembershipStampedAndCopaySplit() {
        when(patientRepository.findByUid(PATIENT)).thenReturn(Optional.of(insuredPatient()));
        when(priceRepository.findCoveredCell(PLAN, ServiceKind.LAB_TEST, LAB, CCY))
                .thenReturn(Optional.of(coveredPlanCell(new BigDecimal("80.00"))));

        // cash 100, plan ceiling 80 -> insurer pays 80, patient co-pay 20.
        CoverageResolution r = resolver.resolve(ServiceKind.LAB_TEST, LAB, PATIENT, CCY, new BigDecimal("100.00"));

        assertThat(r.covered()).isTrue();
        assertThat(r.coveredAmount()).isEqualByComparingTo("80.00");
        assertThat(r.copayAmount()).isEqualByComparingTo("20.00");
        assertThat(r.planUid()).isEqualTo(PLAN);
        assertThat(r.membershipNo()).isEqualTo("MEM-123");
    }

    @Test
    void coveredCeilingAtOrAboveCashYieldsNoCopay() {
        when(patientRepository.findByUid(PATIENT)).thenReturn(Optional.of(insuredPatient()));
        when(priceRepository.findCoveredCell(PLAN, ServiceKind.LAB_TEST, LAB, CCY))
                .thenReturn(Optional.of(coveredPlanCell(new BigDecimal("120.00"))));

        CoverageResolution r = resolver.resolve(ServiceKind.LAB_TEST, LAB, PATIENT, CCY, new BigDecimal("100.00"));

        assertThat(r.covered()).isTrue();
        assertThat(r.copayAmount()).isEqualByComparingTo("0"); // max(100-120, 0)
    }
}
