package com.otapp.hmis.engine.encounter.nursingchart.application;

import com.otapp.hmis.engine.encounter.nursingchart.domain.NursingCarePlanStatus;
import com.otapp.hmis.engine.encounter.nursingchart.domain.WoundStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class NursingChartDtos {

    private NursingChartDtos() {}

    // ---------------------------------------------------------------- vitals

    public record VitalsEntryDto(
            String uid,
            String admissionUid,
            Instant recordedAt,
            String recordedByUsername,
            BigDecimal temperatureC,
            Integer pulseBpm,
            Integer respirationsBpm,
            Integer systolicBp,
            Integer diastolicBp,
            Integer spo2Percent,
            BigDecimal bloodGlucoseMmol,
            Integer painScore,
            String notes,
            Instant createdAt) {}

    public record CreateVitalsEntryRequest(
            BigDecimal temperatureC,
            @Min(0) @Max(300) Integer pulseBpm,
            @Min(0) @Max(120) Integer respirationsBpm,
            @Min(0) @Max(300) Integer systolicBp,
            @Min(0) @Max(300) Integer diastolicBp,
            @Min(0) @Max(100) Integer spo2Percent,
            BigDecimal bloodGlucoseMmol,
            @Min(0) @Max(10) Integer painScore,
            @Size(max = 500) String notes) {}

    // ------------------------------------------------------ care plan items

    public record CarePlanItemDto(
            String uid,
            String admissionUid,
            String problem,
            String goal,
            String intervention,
            String evaluation,
            NursingCarePlanStatus status,
            String openedByUsername,
            Instant openedAt,
            String closedByUsername,
            Instant closedAt,
            String closeReason,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateCarePlanItemRequest(
            @NotBlank @Size(max = 500)  String problem,
            @NotBlank @Size(max = 500)  String goal,
            @NotBlank @Size(max = 2000) String intervention,
            @Size(max = 2000) String evaluation) {}

    public record UpdateCarePlanItemRequest(
            @NotBlank @Size(max = 500)  String problem,
            @NotBlank @Size(max = 500)  String goal,
            @NotBlank @Size(max = 2000) String intervention,
            @Size(max = 2000) String evaluation) {}

    public record ResolveCarePlanItemRequest(@Size(max = 2000) String evaluation) {}

    public record CancelCarePlanItemRequest(@Size(max = 255) String reason) {}

    // -------------------------------------------------------- dressing chart

    public record DressingEntryDto(
            String uid,
            String admissionUid,
            Instant recordedAt,
            String recordedByUsername,
            String woundLocation,
            WoundStatus woundStatus,
            String dressingApplied,
            String notes,
            Instant createdAt) {}

    public record CreateDressingEntryRequest(
            @NotBlank @Size(max = 160) String woundLocation,
            @NotNull  WoundStatus woundStatus,
            @NotBlank @Size(max = 500)  String dressingApplied,
            @Size(max = 1000) String notes) {}

    // -------------------------------------------------------- fluid balance

    public record FluidBalanceEntryDto(
            String uid,
            String admissionUid,
            Instant recordedAt,
            String recordedByUsername,
            Integer intakeMl,
            Integer urineOutputMl,
            Integer drainageOutputMl,
            /** Urine + drainage for this entry. */
            int outputMl,
            /** Intake − total output for this entry (can be negative). */
            int netMl,
            String notes,
            Instant createdAt) {}

    public record CreateFluidBalanceEntryRequest(
            @Min(0) @Max(100000) Integer intakeMl,
            @Min(0) @Max(100000) Integer urineOutputMl,
            @Min(0) @Max(100000) Integer drainageOutputMl,
            @Size(max = 500) String notes) {}

    // -------------------------------------------------------- care activity

    public record CareActivityEntryDto(
            String uid,
            String admissionUid,
            Instant recordedAt,
            String recordedByUsername,
            boolean feedingDone,
            boolean positionChanged,
            boolean bedBathDone,
            BigDecimal randomBloodSugarMmol,
            BigDecimal fastingBloodSugarMmol,
            String notes,
            Instant createdAt) {}

    public record CreateCareActivityEntryRequest(
            boolean feedingDone,
            boolean positionChanged,
            boolean bedBathDone,
            BigDecimal randomBloodSugarMmol,
            BigDecimal fastingBloodSugarMmol,
            @Size(max = 500) String notes) {}
}
