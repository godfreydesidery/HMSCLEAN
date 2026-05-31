package com.otapp.hmis.engine.encounter.result.application;

import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.result.domain.LabResultFlag;
import com.otapp.hmis.engine.encounter.result.domain.OrderResultStatus;
import com.otapp.hmis.engine.masterdata.labtest.domain.AnalyteValueKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderResultDtos {

    private OrderResultDtos() {}

    public record OrderResultDto(
            Long id,
            String uid,
            String orderUid,
            ClinicalOrderKind orderKind,
            OrderResultStatus status,
            String narrative,
            String impression,
            List<LabResultLineDto> lines,
            Instant finalizedAt,
            String finalizedBy,
            Instant amendedAt,
            String amendedBy,
            Instant createdAt,
            Instant updatedAt) {}

    /** A measured analyte value on a lab result, with snapshotted range + computed flag. */
    public record LabResultLineDto(
            Long id, String uid, String analyteUid, String analyteCode, String analyteName,
            AnalyteValueKind valueKind, BigDecimal valueNumeric, String valueText, String unit,
            LabResultFlag flag, BigDecimal refLow, BigDecimal refHigh,
            BigDecimal criticalLow, BigDecimal criticalHigh, String rangeDisplay,
            int displayOrder, String note) {}

    public record SaveResultRequest(
            @Size(max = 8000) String narrative,
            @Size(max = 1000) String impression,
            @Valid List<LabResultLineInput> lines) {}

    /** One submitted analyte value for a lab result. */
    public record LabResultLineInput(
            @NotBlank String analyteUid,
            @Digits(integer = 10, fraction = 4) BigDecimal valueNumeric,
            @Size(max = 2000) String valueText,
            @Size(max = 500) String note) {}
}
