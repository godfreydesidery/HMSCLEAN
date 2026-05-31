package com.otapp.hmis.engine.encounter.prescription.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertDtos.AlertKind;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertDtos.PrescribingAlertDto;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertDtos.PrescribingAlertsDto;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.encounter.vitals.domain.VitalsCalculator;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineForm;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PatientType;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Focused coverage of the clinical-note-safety logic:
 * <ul>
 *   <li>{@link VitalsCalculator} — pure BMI / BSA(Mosteller) derivation, NUMERIC, never float.</li>
 *   <li>{@link PrescribingAlertService} — the two non-blocking advisories
 *       (same-medicine-this-month + unfinished-course) over SOLD prescriptions,
 *       including the empty / never-given paths and the 404-on-unknown-uid path.</li>
 * </ul>
 * No Spring context, no DB — mocks for the alert service; pure calls for the calculator.
 */
@ExtendWith(MockitoExtension.class)
class ClinicalNoteSafetyTest {

    private static final String PATIENT_UID  = "01HPATIENT0000000000000AAA";
    private static final String MEDICINE_UID = "01HMEDICINE00000000000AAAA";

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private MedicineRepository medicineRepository;

    private PrescribingAlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new PrescribingAlertService(prescriptionRepository, patientRepository, medicineRepository);
    }

    // ---------------------------------------------------------------------
    // VitalsCalculator — pure BMI / BSA
    // ---------------------------------------------------------------------

    @Test
    void bmiIsWeightOverHeightSquaredRoundedToOneDecimal() {
        // 70 kg, 175 cm -> 70 / 1.75^2 = 22.857... -> 22.9
        assertThat(VitalsCalculator.bmi(new BigDecimal("70.0"), new BigDecimal("175.0")))
                .isEqualByComparingTo("22.9");
    }

    @Test
    void bsaMostellerIsSqrtOfHeightTimesWeightOver3600() {
        // sqrt(175 * 70 / 3600) = sqrt(3.40277...) = 1.8447... -> 1.84
        assertThat(VitalsCalculator.bsaMosteller(new BigDecimal("70.0"), new BigDecimal("175.0")))
                .isEqualByComparingTo("1.84");
    }

    @Test
    void bmiAndBsaAreNullWhenAnyInputMissingOrNonPositive() {
        assertThat(VitalsCalculator.bmi(null, new BigDecimal("175"))).isNull();
        assertThat(VitalsCalculator.bmi(new BigDecimal("70"), null)).isNull();
        assertThat(VitalsCalculator.bmi(new BigDecimal("70"), BigDecimal.ZERO)).isNull();
        assertThat(VitalsCalculator.bsaMosteller(null, new BigDecimal("175"))).isNull();
        assertThat(VitalsCalculator.bsaMosteller(new BigDecimal("70"), BigDecimal.ZERO)).isNull();
    }

    // ---------------------------------------------------------------------
    // PrescribingAlertService — same-medicine-this-month
    // ---------------------------------------------------------------------

    @Test
    void sameMedicineThisMonthFiresWhenLastGivenWithin30Days() {
        Prescription last = sold(10, Instant.now().minus(5, ChronoUnit.DAYS));
        when(prescriptionRepository.findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(
                PATIENT_UID, MEDICINE_UID, PrescriptionStatus.SOLD)).thenReturn(List.of(last));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.of(medicine()));

        Optional<PrescribingAlertDto> alert = alertService.sameMedicineThisMonth(PATIENT_UID, MEDICINE_UID);

        assertThat(alert).isPresent();
        assertThat(alert.get().kind()).isEqualTo(AlertKind.SAME_MEDICINE_THIS_MONTH);
        assertThat(alert.get().daysSinceLastGiven()).isEqualTo(5);
        assertThat(alert.get().message()).contains("Has drugs this month");
    }

    @Test
    void sameMedicineThisMonthSilentWhenOlderThanAMonth() {
        Prescription last = sold(10, Instant.now().minus(40, ChronoUnit.DAYS));
        when(prescriptionRepository.findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(
                PATIENT_UID, MEDICINE_UID, PrescriptionStatus.SOLD)).thenReturn(List.of(last));

        assertThat(alertService.sameMedicineThisMonth(PATIENT_UID, MEDICINE_UID)).isEmpty();
    }

    @Test
    void sameMedicineThisMonthSilentWhenNeverGiven() {
        when(prescriptionRepository.findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(
                PATIENT_UID, MEDICINE_UID, PrescriptionStatus.SOLD)).thenReturn(List.of());

        assertThat(alertService.sameMedicineThisMonth(PATIENT_UID, MEDICINE_UID)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // PrescribingAlertService — unfinished-course
    // ---------------------------------------------------------------------

    @Test
    void unfinishedCourseFiresWhenElapsedLessThanDuration() {
        // 10-day course dispensed 3 days ago -> 7 days remaining
        Prescription last = sold(10, Instant.now().minus(3, ChronoUnit.DAYS));
        when(prescriptionRepository.findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(
                PATIENT_UID, MEDICINE_UID, PrescriptionStatus.SOLD)).thenReturn(List.of(last));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.of(medicine()));

        Optional<PrescribingAlertDto> alert = alertService.unfinishedCourse(PATIENT_UID, MEDICINE_UID);

        assertThat(alert).isPresent();
        assertThat(alert.get().kind()).isEqualTo(AlertKind.UNFINISHED_COURSE);
        assertThat(alert.get().durationDays()).isEqualTo(10);
        assertThat(alert.get().daysRemaining()).isEqualTo(7);
        assertThat(alert.get().message()).contains("has not completed the last prescription");
    }

    @Test
    void unfinishedCourseSilentWhenCourseFinished() {
        // 10-day course dispensed 15 days ago -> finished, no alert
        Prescription last = sold(10, Instant.now().minus(15, ChronoUnit.DAYS));
        when(prescriptionRepository.findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(
                PATIENT_UID, MEDICINE_UID, PrescriptionStatus.SOLD)).thenReturn(List.of(last));

        assertThat(alertService.unfinishedCourse(PATIENT_UID, MEDICINE_UID)).isEmpty();
    }

    @Test
    void unfinishedCourseSilentWhenDurationMissing() {
        Prescription last = sold(null, Instant.now().minus(1, ChronoUnit.DAYS));
        when(prescriptionRepository.findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(
                PATIENT_UID, MEDICINE_UID, PrescriptionStatus.SOLD)).thenReturn(List.of(last));

        assertThat(alertService.unfinishedCourse(PATIENT_UID, MEDICINE_UID)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // PrescribingAlertService.alertsFor — combined + 404 paths
    // ---------------------------------------------------------------------

    @Test
    void alertsForReturnsBothWhenInProgressWithinMonth() {
        Prescription last = sold(10, Instant.now().minus(2, ChronoUnit.DAYS));
        when(patientRepository.findByUid(PATIENT_UID)).thenReturn(Optional.of(patient()));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.of(medicine()));
        when(prescriptionRepository.findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(
                PATIENT_UID, MEDICINE_UID, PrescriptionStatus.SOLD)).thenReturn(List.of(last));

        PrescribingAlertsDto dto = alertService.alertsFor(PATIENT_UID, MEDICINE_UID);

        assertThat(dto.alerts()).hasSize(2);
        assertThat(dto.alerts()).extracting(PrescribingAlertDto::kind)
                .containsExactlyInAnyOrder(AlertKind.SAME_MEDICINE_THIS_MONTH, AlertKind.UNFINISHED_COURSE);
    }

    @Test
    void alertsForReturnsEmptyListWhenNoHistory() {
        when(patientRepository.findByUid(PATIENT_UID)).thenReturn(Optional.of(patient()));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.of(medicine()));
        when(prescriptionRepository.findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(
                PATIENT_UID, MEDICINE_UID, PrescriptionStatus.SOLD)).thenReturn(List.of());

        assertThat(alertService.alertsFor(PATIENT_UID, MEDICINE_UID).alerts()).isEmpty();
    }

    @Test
    void alertsForThrowsNotFoundWhenPatientUnknown() {
        when(patientRepository.findByUid(PATIENT_UID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.alertsFor(PATIENT_UID, MEDICINE_UID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void alertsForThrowsNotFoundWhenMedicineUnknown() {
        when(patientRepository.findByUid(PATIENT_UID)).thenReturn(Optional.of(patient()));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.alertsFor(PATIENT_UID, MEDICINE_UID))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------------

    private static Prescription sold(Integer durationDays, Instant approvedAt) {
        Prescription p = new Prescription("RX-1", "01HCONSULT00000000000AAAAA", PATIENT_UID,
                MEDICINE_UID, "1 tab", "BD", durationDays, 20, null);
        p.setStatus(PrescriptionStatus.SOLD);
        p.setApprovedAt(approvedAt);
        return p;
    }

    private static Medicine medicine() {
        Medicine m = new Medicine("MED1", "Amoxicillin", null, "500mg", MedicineForm.TABLET, null);
        ReflectionTestUtils.setField(
                m, com.otapp.hmis.engine.common.persistence.AuditableEntity.class, "uid", MEDICINE_UID, String.class);
        return m;
    }

    private static Patient patient() {
        return new Patient("PT-1", "Jane", null, "Doe",
                LocalDate.of(1990, 1, 1), Gender.FEMALE, PatientType.OUTPATIENT, PaymentType.CASH);
    }
}
