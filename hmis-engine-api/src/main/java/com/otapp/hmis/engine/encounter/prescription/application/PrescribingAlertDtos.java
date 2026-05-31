package com.otapp.hmis.engine.encounter.prescription.application;

import java.time.Instant;
import java.util.List;

/**
 * Advisory, non-persistent prescribing alerts (legacy PatientResource
 * get_same_medicine_alert_one_month_by_prescription_id /
 * get_unfinished_medicine_alert_by_patient_id_and_medicine_id). These are
 * read-only, never block a prescription, and never throw on a data gap — the
 * prescriber sees them and decides. The HARD stop is the duplicate-drug guard.
 */
public final class PrescribingAlertDtos {

    private PrescribingAlertDtos() {}

    public enum AlertKind { SAME_MEDICINE_THIS_MONTH, UNFINISHED_COURSE }

    public enum AlertSeverity { INFO, WARN }

    public record PrescribingAlertDto(
            AlertKind kind,
            AlertSeverity severity,
            String message,
            String medicineUid,
            String medicineName,
            Instant lastGivenAt,
            Integer daysSinceLastGiven,
            Integer durationDays,
            Integer daysRemaining) {}

    /** Wrapper for the pre-prescribe advisory check; list may be empty. */
    public record PrescribingAlertsDto(
            String patientUid,
            String medicineUid,
            String medicineName,
            List<PrescribingAlertDto> alerts) {}
}
