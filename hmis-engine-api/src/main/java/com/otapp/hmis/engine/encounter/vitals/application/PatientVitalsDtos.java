package com.otapp.hmis.engine.encounter.vitals.application;

import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus;
import com.otapp.hmis.engine.encounter.vitals.domain.VitalsStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class PatientVitalsDtos {

    private PatientVitalsDtos() {}

    public record PatientVitalsDto(
            Long id,
            String uid,
            String consultationUid,
            String patientUid,
            VitalsStatus status,
            Instant takenAt,
            BigDecimal temperatureC,
            Integer pulseBpm,
            Integer respirationBpm,
            Integer bloodPressureSystolic,
            Integer bloodPressureDiastolic,
            Integer spo2Percent,
            BigDecimal weightKg,
            BigDecimal heightCm,
            BigDecimal bmi,
            BigDecimal bsa,
            String bmiComment,
            String notes,
            Instant submittedAt,
            Instant archivedAt,
            Instant createdAt,
            Instant updatedAt) {}

    /**
     * One row of the outpatient nurse-triage worklist: a fee-settled consultation
     * that may need vitals, enriched with who the patient is and the current
     * status of their vitals capture (null when no row has been materialised yet).
     */
    public record VitalsWorklistRow(
            Long consultationId,
            String consultationUid,
            String consultationNo,
            String patientUid,
            String patientNo,
            String patientName,
            ConsultationStatus consultationStatus,
            boolean feeSettled,
            String vitalsUid,
            VitalsStatus vitalsStatus,
            Instant bookedAt,
            Instant startedAt) {}

    public record RecordVitalsRequest(
            @DecimalMin("25.0") @DecimalMax("45.0")  BigDecimal temperatureC,
            @Min(20) @Max(250)  Integer pulseBpm,
            @Min(5)  @Max(80)   Integer respirationBpm,
            @Min(40) @Max(260)  Integer bloodPressureSystolic,
            @Min(20) @Max(200)  Integer bloodPressureDiastolic,
            @Min(40) @Max(100)  Integer spo2Percent,
            @DecimalMin("0.5") @DecimalMax("400.0") BigDecimal weightKg,
            @DecimalMin("20.0") @DecimalMax("260.0") BigDecimal heightCm,
            @DecimalMin("10.0") @DecimalMax("80.0")  BigDecimal bmi,
            @DecimalMin("0.1")  @DecimalMax("4.0")   BigDecimal bsa,
            @Size(max = 255) String bmiComment,
            @Size(max = 500) String notes) {}
}
