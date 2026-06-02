package com.otapp.hmis.engine.transfer.pharmacystorereturn.application;

import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturnStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PharmacyStoreReturnDtos {

    private PharmacyStoreReturnDtos() {}

    // ----- requests --------------------------------------------------------

    public record CreateReturnRequest(
            @NotBlank @Size(min = 26, max = 26) String pharmacyUid,
            @NotBlank @Size(min = 26, max = 26) String storeUid,
            LocalDate returnDate,
            @Size(max = 500) String reason,
            @Size(max = 500) String note,
            @NotEmpty @Valid List<CreateReturnLineRequest> lines) {}

    public record CreateReturnLineRequest(
            @NotBlank @Size(min = 26, max = 26) String medicineUid,
            /** Optional. If null, {@code quantity} is treated as base units. */
            @Size(min = 26, max = 26) String unitUid,
            @Min(1) int quantity,
            @Size(max = 500) String reason) {}

    public record ReasonRequest(@Size(max = 255) String reason) {}

    // ----- responses -------------------------------------------------------

    public record ReturnLineDto(
            String uid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            String unitUid,
            String unitCode,
            int unitFactorToBase,
            int quantity,
            String reason,
            List<BatchPickDto> picks,
            Instant createdAt) {}

    public record BatchPickDto(
            String batchUid,
            String batchNo,
            LocalDate manufacturedDate,
            LocalDate expiresAt,
            int quantity) {}

    public record ReturnDto(
            String uid,
            String returnNo,
            String pharmacyUid,
            String pharmacyName,
            String storeUid,
            String storeName,
            LocalDate returnDate,
            String reason,
            String note,
            PharmacyStoreReturnStatus status,
            Instant submittedAt,
            String submittedByUsername,
            Instant completedAt,
            String completedByUsername,
            Instant rejectedAt,
            String rejectedByUsername,
            String rejectReason,
            Instant cancelledAt,
            Instant createdAt,
            Instant updatedAt,
            List<ReturnLineDto> lines) {}

    public record ReturnSummary(
            String uid,
            String returnNo,
            String pharmacyName,
            String storeName,
            LocalDate returnDate,
            PharmacyStoreReturnStatus status,
            int lineCount,
            Instant createdAt) {}
}
