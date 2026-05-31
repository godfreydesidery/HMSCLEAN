package com.otapp.hmis.engine.masterdata.labtest.application;

import com.otapp.hmis.engine.masterdata.labtest.domain.AnalyteValueKind;
import com.otapp.hmis.engine.masterdata.labtest.domain.RangeSex;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class LabTestAnalyteDtos {

    private LabTestAnalyteDtos() {}

    // ----- read DTOs (carry id + uid; id for client-side joins, never in URLs) -----

    public record LabTestAnalyteDto(
            Long id, String uid, String labTestTypeUid, String code, String name, String unit,
            AnalyteValueKind valueKind, int displayOrder, boolean active,
            List<LabReferenceRangeDto> ranges, Instant createdAt, Instant updatedAt) {}

    public record LabReferenceRangeDto(
            Long id, String uid, String analyteUid, RangeSex sex, Integer ageMinDays, Integer ageMaxDays,
            @Digits(integer = 10, fraction = 4) BigDecimal refLow,
            @Digits(integer = 10, fraction = 4) BigDecimal refHigh,
            @Digits(integer = 10, fraction = 4) BigDecimal criticalLow,
            @Digits(integer = 10, fraction = 4) BigDecimal criticalHigh,
            String normalText, String rangeDisplay, boolean active) {}

    // ----- write requests -----

    public record CreateAnalyteRequest(
            @NotBlank @Size(min = 1, max = 32) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 32) String unit,
            @NotNull AnalyteValueKind valueKind,
            @PositiveOrZero int displayOrder) {}

    public record UpdateAnalyteRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 32) String unit,
            @NotNull AnalyteValueKind valueKind,
            @PositiveOrZero int displayOrder,
            boolean active) {}

    public record CreateRangeRequest(
            @NotNull RangeSex sex,
            @PositiveOrZero Integer ageMinDays,
            @PositiveOrZero Integer ageMaxDays,
            @Digits(integer = 10, fraction = 4) BigDecimal refLow,
            @Digits(integer = 10, fraction = 4) BigDecimal refHigh,
            @Digits(integer = 10, fraction = 4) BigDecimal criticalLow,
            @Digits(integer = 10, fraction = 4) BigDecimal criticalHigh,
            @Size(max = 200) String normalText,
            @Size(max = 120) String rangeDisplay) {}

    public record UpdateRangeRequest(
            @NotNull RangeSex sex,
            @PositiveOrZero Integer ageMinDays,
            @PositiveOrZero Integer ageMaxDays,
            @Digits(integer = 10, fraction = 4) BigDecimal refLow,
            @Digits(integer = 10, fraction = 4) BigDecimal refHigh,
            @Digits(integer = 10, fraction = 4) BigDecimal criticalLow,
            @Digits(integer = 10, fraction = 4) BigDecimal criticalHigh,
            @Size(max = 200) String normalText,
            @Size(max = 120) String rangeDisplay,
            boolean active) {}

    // ----- cross-module read views (consumed by the encounter result module) -----

    /** Lightweight analyte definition for building a result-entry grid. */
    public record AnalyteTemplateDto(
            String uid, String code, String name, String unit,
            AnalyteValueKind valueKind, int displayOrder) {}

    /**
     * The analyte definition plus the reference range that applies to a given
     * patient (sex + age), resolved at result-entry time so the encounter
     * module can snapshot it onto the result line. {@code referenceRangeUid} is
     * null when no range matched the patient demographic.
     */
    public record AnalyteResolutionDto(
            String analyteUid, String labTestTypeUid, String code, String name, String unit,
            AnalyteValueKind valueKind, int displayOrder,
            @Digits(integer = 10, fraction = 4) BigDecimal refLow,
            @Digits(integer = 10, fraction = 4) BigDecimal refHigh,
            @Digits(integer = 10, fraction = 4) BigDecimal criticalLow,
            @Digits(integer = 10, fraction = 4) BigDecimal criticalHigh,
            String normalText, String rangeDisplay, String referenceRangeUid) {}
}
