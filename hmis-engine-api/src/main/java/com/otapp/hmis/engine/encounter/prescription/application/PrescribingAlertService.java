package com.otapp.hmis.engine.encounter.prescription.application;

import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertDtos.AlertKind;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertDtos.AlertSeverity;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertDtos.PrescribingAlertDto;
import com.otapp.hmis.engine.encounter.prescription.application.PrescribingAlertDtos.PrescribingAlertsDto;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Advisory prescribing alerts (legacy PatientResource
 * get_same_medicine_alert_one_month_by_prescription_id /
 * get_unfinished_medicine_alert_by_patient_id_and_medicine_id).
 *
 * <p>Both alerts read the patient's already-DISPENSED (legacy "GIVEN" == SOLD)
 * prescriptions for one medicine and compute timing from {@code approvedAt}.
 * They are non-blocking and never throw on a history gap (legacy wrapped the
 * body in try/catch → empty). The only failure surfaced is 404 when the
 * patient or medicine uid itself is unknown.
 */
@Service
@RequiredArgsConstructor
public class PrescribingAlertService {

    /** SOLD is the rewrite equivalent of the legacy "GIVEN" (dispensed) status. */
    private static final PrescriptionStatus GIVEN = PrescriptionStatus.SOLD;

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;

    /**
     * Both advisories for a (patient, medicine) pair, ready for the pre-prescribe
     * GET. 404 if either uid is unknown; otherwise a (possibly empty) list.
     */
    @Transactional(readOnly = true)
    public PrescribingAlertsDto alertsFor(String patientUid, String medicineUid) {
        if (patientRepository.findByUid(patientUid).isEmpty()) {
            throw new NotFoundException("Patient not found: " + patientUid);
        }
        Medicine medicine = medicineRepository.findByUid(medicineUid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + medicineUid));

        List<PrescribingAlertDto> alerts = new ArrayList<>();
        sameMedicineThisMonth(patientUid, medicineUid).ifPresent(alerts::add);
        unfinishedCourse(patientUid, medicineUid).ifPresent(alerts::add);
        return new PrescribingAlertsDto(patientUid, medicineUid, medicine.getName(), alerts);
    }

    /**
     * SAME_MEDICINE_THIS_MONTH (legacy get_same_medicine_alert_one_month...):
     * the medicine was last dispensed within the last 30 days (or only hours
     * ago). Advisory only; empty when never dispensed or older than a month.
     */
    @Transactional(readOnly = true)
    public Optional<PrescribingAlertDto> sameMedicineThisMonth(String patientUid, String medicineUid) {
        try {
            Prescription last = lastGiven(patientUid, medicineUid);
            if (last == null || last.getApprovedAt() == null) {
                return Optional.empty();
            }
            Instant lastGivenAt = last.getApprovedAt();
            long days = ChronoUnit.DAYS.between(lastGivenAt, Instant.now());
            long hours = ChronoUnit.HOURS.between(lastGivenAt, Instant.now());

            String medicineName = medicineName(medicineUid);
            String message;
            boolean thisMonth;
            if (days > 0) {
                message = medicineName + " | Last given " + days + " day(s) ago. (" + lastGivenAt + ")";
                thisMonth = days <= 30;
                if (thisMonth) {
                    message += " Has drugs this month.";
                }
            } else {
                message = medicineName + " | Last given " + hours + " hour(s) ago. (" + lastGivenAt + ")"
                        + " Has drugs this month.";
                thisMonth = true;
            }
            if (!thisMonth) {
                return Optional.empty();
            }
            return Optional.of(new PrescribingAlertDto(
                    AlertKind.SAME_MEDICINE_THIS_MONTH,
                    AlertSeverity.INFO,
                    message,
                    medicineUid,
                    medicineName,
                    lastGivenAt,
                    (int) days,
                    null,
                    null));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * UNFINISHED_COURSE (legacy get_unfinished_medicine_alert...): the days
     * elapsed since the last dispense is fewer than that course's
     * durationDays, so the patient should still be on it. Advisory only;
     * empty when no course is in progress.
     */
    @Transactional(readOnly = true)
    public Optional<PrescribingAlertDto> unfinishedCourse(String patientUid, String medicineUid) {
        try {
            Prescription last = lastGiven(patientUid, medicineUid);
            if (last == null || last.getApprovedAt() == null || last.getDurationDays() == null) {
                return Optional.empty();
            }
            int durationDays = last.getDurationDays();
            long elapsed = ChronoUnit.DAYS.between(last.getApprovedAt(), Instant.now());
            if (elapsed >= durationDays) {
                return Optional.empty();
            }
            int remaining = (int) (durationDays - elapsed);
            String medicineName = medicineName(medicineUid);
            String message = "The patient has not completed the last prescription. There are " + remaining
                    + " day(s) left to finish this medicine. Was prescribed on " + last.getApprovedAt()
                    + " for " + durationDays + " day(s).";
            return Optional.of(new PrescribingAlertDto(
                    AlertKind.UNFINISHED_COURSE,
                    AlertSeverity.WARN,
                    message,
                    medicineUid,
                    medicineName,
                    last.getApprovedAt(),
                    (int) elapsed,
                    durationDays,
                    remaining));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * The most-recently dispensed (SOLD) prescription for this patient+medicine.
     * Legacy iterated the list and kept the last element; the repository orders
     * newest-dispensed first, so the head is that "last" row.
     */
    private Prescription lastGiven(String patientUid, String medicineUid) {
        List<Prescription> given = prescriptionRepository
                .findAllByPatientUidAndMedicineUidAndStatusOrderByApprovedAtDesc(patientUid, medicineUid, GIVEN);
        return given.isEmpty() ? null : given.get(0);
    }

    private String medicineName(String medicineUid) {
        return medicineRepository.findByUid(medicineUid).map(Medicine::getName).orElse(medicineUid);
    }
}
