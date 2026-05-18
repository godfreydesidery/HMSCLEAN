package com.otapp.hmis.engine.transfer.pharmacystore.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request Order (RO): a pharmacy asks a central store for stock. Once
 * APPROVED + SUBMITTED, the store can fulfil it with a
 * {@link StoreToPharmacyTO}. The RO header carries the workflow gates; line
 * detail lives on {@link PharmacyToStoreROLine}.
 */
@Entity
@Table(name = "pharmacy_to_store_ro",
       uniqueConstraints = @UniqueConstraint(name = "uk_pharmacy_to_store_ro_no", columnNames = "ro_no"),
       indexes = {
               @Index(name = "idx_p2s_ro_pharmacy", columnList = "pharmacy_uid"),
               @Index(name = "idx_p2s_ro_store",    columnList = "store_uid"),
               @Index(name = "idx_p2s_ro_status",   columnList = "status"),
               @Index(name = "idx_p2s_ro_order_date", columnList = "order_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyToStoreRO extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable RO number, e.g. P2S-RO-2026-000123. */
    @Column(name = "ro_no", nullable = false, length = 32)
    private String roNo;

    @Column(name = "pharmacy_uid", nullable = false, length = 26) private String pharmacyUid;
    @Column(name = "store_uid",    nullable = false, length = 26) private String storeUid;

    @Column(name = "order_date", nullable = false) private LocalDate orderDate;
    @Setter @Column(name = "valid_until")          private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferDocStatus status = TransferDocStatus.PENDING;

    @Setter @Column(name = "verified_at")    private Instant verifiedAt;
    @Setter @Column(name = "approved_at")    private Instant approvedAt;
    @Setter @Column(name = "submitted_at")   private Instant submittedAt;
    @Setter @Column(name = "in_process_at")  private Instant inProcessAt;
    @Setter @Column(name = "issued_at")      private Instant issuedAt;
    @Setter @Column(name = "completed_at")   private Instant completedAt;
    @Setter @Column(name = "rejected_at")    private Instant rejectedAt;
    @Setter @Column(name = "returned_at")    private Instant returnedAt;
    @Setter @Column(name = "reject_reason", length = 255) private String rejectReason;
    @Setter @Column(name = "note",          length = 500) private String note;

    public PharmacyToStoreRO(String roNo, String pharmacyUid, String storeUid,
                             LocalDate orderDate, LocalDate validUntil, String note) {
        this.roNo = roNo;
        this.pharmacyUid = pharmacyUid;
        this.storeUid = storeUid;
        this.orderDate = orderDate == null ? LocalDate.now() : orderDate;
        this.validUntil = validUntil;
        this.note = note;
    }

    // ----- state transitions ------------------------------------------------

    public void verify() {
        requireStatus("verify", TransferDocStatus.PENDING);
        status = TransferDocStatus.VERIFIED;
        verifiedAt = Instant.now();
    }

    public void approve() {
        requireStatus("approve", TransferDocStatus.PENDING, TransferDocStatus.VERIFIED);
        status = TransferDocStatus.APPROVED;
        approvedAt = Instant.now();
    }

    public void submit() {
        requireStatus("submit", TransferDocStatus.APPROVED);
        status = TransferDocStatus.SUBMITTED;
        submittedAt = Instant.now();
    }

    /** Store has picked up the RO and started preparing a TO against it. */
    public void markInProcess() {
        if (status == TransferDocStatus.IN_PROCESS) return;
        requireStatus("start processing", TransferDocStatus.SUBMITTED);
        status = TransferDocStatus.IN_PROCESS;
        inProcessAt = Instant.now();
    }

    /** Store has shipped (TO reached GOODS_ISSUED). */
    public void markGoodsIssued() {
        if (status == TransferDocStatus.GOODS_ISSUED) return;
        requireStatus("mark goods issued",
                TransferDocStatus.SUBMITTED, TransferDocStatus.IN_PROCESS);
        status = TransferDocStatus.GOODS_ISSUED;
        issuedAt = Instant.now();
    }

    /** Receiving pharmacy has signed the RN. */
    public void markCompleted() {
        if (status == TransferDocStatus.COMPLETED) return;
        requireStatus("mark complete", TransferDocStatus.GOODS_ISSUED);
        status = TransferDocStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void reject(String reason) {
        if (isTerminal()) {
            throw new BusinessRuleException("Cannot reject a " + status + " RO");
        }
        status = TransferDocStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectReason = reason;
    }

    public void returnToRequester(String reason) {
        if (isTerminal()) {
            throw new BusinessRuleException("Cannot return a " + status + " RO");
        }
        status = TransferDocStatus.RETURNED;
        returnedAt = Instant.now();
        rejectReason = reason;
    }

    public boolean isTerminal() {
        return status == TransferDocStatus.COMPLETED
                || status == TransferDocStatus.REJECTED
                || status == TransferDocStatus.RETURNED;
    }

    public boolean isEditable() {
        return status == TransferDocStatus.PENDING;
    }

    private void requireStatus(String op, TransferDocStatus... allowed) {
        for (TransferDocStatus s : allowed) {
            if (status == s) return;
        }
        throw new BusinessRuleException(
                "Cannot " + op + " RO from status " + status);
    }
}
