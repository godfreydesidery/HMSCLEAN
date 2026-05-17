package com.otapp.hmis.engine.encounter.vitals.application;

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
            String uid,
            String consultationUid,
            String patientUid,
            Instant takenAt,
            BigDecimal temperatureC,
            Integer pulseBpm,
            Integer respirationBpm,
            Integer bloodPressureSystolic,
            Integer bloodPressureDiastolic,
            Integer spo2Percent,
            BigDecimal weightKg,
            BigDecimal heightCm,
            String notes,
            Instant createdAt,
            Instant updatedAt) {}

    public record RecordVitalsRequest(
            @DecimalMin("25.0") @DecimalMax("45.0")  BigDecimal temperatureC,
            @Min(20) @Max(250)  Integer pulseBpm,
            @Min(5)  @Max(80)   Integer respirationBpm,
            @Min(40) @Max(260)  Integer bloodPressureSystolic,
            @Min(20) @Max(200)  Integer bloodPressureDiastolic,
            @Min(40) @Max(100)  Integer spo2Percent,
            @DecimalMin("0.5") @DecimalMax("400.0") BigDecimal weightKg,
            @DecimalMin("20.0") @DecimalMax("260.0") BigDecimal heightCm,
            @Size(max = 500) String notes) {}
}
