package com.otapp.hmis.engine.transfer.pharmacystore.application;

import com.otapp.hmis.engine.transfer.common.domain.ReceiveNoteStatus;
import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PharmacyStoreTransferDtos {

    private PharmacyStoreTransferDtos() {}

    // ----- requests --------------------------------------------------------

    public record CreateRORequest(
            @NotBlank @Size(min = 26, max = 26) String pharmacyUid,
            @NotBlank @Size(min = 26, max = 26) String storeUid,
            LocalDate validUntil,
            @Size(max = 500) String note,
            @NotEmpty @Valid List<CreateROLineRequest> lines) {}

    public record CreateROLineRequest(
            @NotBlank @Size(min = 26, max = 26) String medicineUid,
            /** Optional. If null, {@code quantity} is treated as already in base units. */
            @Size(min = 26, max = 26) String unitUid,
            @Min(1) int quantity,
            @Size(max = 500) String note) {}

    public record CreateTORequest(
            @NotBlank @Size(min = 26, max = 26) String roUid,
            @Size(max = 500) String note,
            @NotEmpty @Valid List<CreateTOLineRequest> lines) {}

    /**
     * One line on a Transfer Order. {@code quantity} is what the store is
     * committing to ship for the matching RO line. If the RO line was
     * created with a {@code unitUid}, this quantity is in that same unit
     * — the service converts to base before checking against outstanding.
     */
    public record CreateTOLineRequest(
            @NotBlank @Size(min = 26, max = 26) String roLineUid,
            @Min(1) int quantity) {}

    public record CreateRNRequest(
            @NotBlank @Size(min = 26, max = 26) String toUid,
            LocalDate receivingDate,
            @Size(max = 500) String note,
            @NotEmpty @Valid List<CreateRNLineRequest> lines) {}

    /**
     * One line on a Receive Note. {@code receivedQuantity} may be less than
     * the matching TO line's issued amount — the difference is recorded as
     * a transit shortfall on the RN line and does not reach pharmacy stock.
     * Quantity is interpreted in the source TO line's unit (or base units
     * if the line was created without one).
     */
    public record CreateRNLineRequest(
            @NotBlank @Size(min = 26, max = 26) String toLineUid,
            @NotNull @Min(0) Integer receivedQuantity) {}

    public record ReasonRequest(@Size(max = 255) String reason) {}

    // ----- responses -------------------------------------------------------

    public record ROLineDto(
            String uid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            String unitUid,
            String unitCode,
            int unitFactorToBase,
            int requestedQuantity,
            int fulfilledQuantity,
            int outstandingQuantity,
            String note,
            Instant createdAt) {}

    public record RODto(
            String uid,
            String roNo,
            String pharmacyUid,
            String pharmacyName,
            String storeUid,
            String storeName,
            LocalDate orderDate,
            LocalDate validUntil,
            TransferDocStatus status,
            Instant verifiedAt,
            Instant approvedAt,
            Instant submittedAt,
            Instant inProcessAt,
            Instant issuedAt,
            Instant completedAt,
            Instant rejectedAt,
            Instant returnedAt,
            String rejectReason,
            String note,
            Instant createdAt,
            Instant updatedAt,
            List<ROLineDto> lines) {}

    public record ROSummary(
            String uid,
            String roNo,
            String pharmacyName,
            String storeName,
            LocalDate orderDate,
            TransferDocStatus status,
            int lineCount,
            Instant createdAt) {}

    public record TOBatchPickDto(
            String batchUid,
            String batchNo,
            LocalDate manufacturedDate,
            LocalDate expiresAt,
            int quantity,
            String rnLineUid) {}

    public record TOLineDto(
            String uid,
            String roLineUid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            String unitUid,
            String unitCode,
            int unitFactorToBase,
            int requestedQuantity,
            int issuedQuantity,
            int receivedQuantity,
            List<TOBatchPickDto> picks,
            Instant createdAt) {}

    public record TODto(
            String uid,
            String toNo,
            String roUid,
            String roNo,
            String pharmacyUid,
            String pharmacyName,
            String storeUid,
            String storeName,
            LocalDate orderDate,
            TransferDocStatus status,
            Instant verifiedAt,
            Instant approvedAt,
            Instant issuedAt,
            Instant completedAt,
            Instant rejectedAt,
            String rejectedReason,
            String note,
            Instant createdAt,
            Instant updatedAt,
            List<TOLineDto> lines) {}

    public record TOSummary(
            String uid,
            String toNo,
            String roNo,
            String pharmacyName,
            String storeName,
            LocalDate orderDate,
            TransferDocStatus status,
            int lineCount,
            Instant createdAt) {}

    public record RNLineDto(
            String uid,
            String toLineUid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            String unitUid,
            String unitCode,
            int unitFactorToBase,
            int issuedQuantity,
            int receivedQuantity,
            int shortfall,
            Instant createdAt) {}

    public record RNDto(
            String uid,
            String rnNo,
            String toUid,
            String toNo,
            String pharmacyUid,
            String pharmacyName,
            String storeUid,
            String storeName,
            LocalDate receivingDate,
            ReceiveNoteStatus status,
            Instant completedAt,
            Instant cancelledAt,
            String note,
            Instant createdAt,
            Instant updatedAt,
            List<RNLineDto> lines) {}

    public record RNSummary(
            String uid,
            String rnNo,
            String toNo,
            String pharmacyName,
            String storeName,
            LocalDate receivingDate,
            ReceiveNoteStatus status,
            int lineCount,
            Instant createdAt) {}
}
