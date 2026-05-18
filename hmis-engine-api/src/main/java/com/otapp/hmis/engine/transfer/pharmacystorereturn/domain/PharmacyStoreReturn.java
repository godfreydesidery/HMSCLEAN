package com.otapp.hmis.engine.transfer.pharmacystorereturn.domain;

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
 * One pharmacy returning surplus stock back to the central store
 * (PROCESS.md §8.5 reverse direction). Single-document flow — simpler
 * than the three-doc RO/TO/RN dance the forward path uses, because the
 * pharmacy has full information about what it's sending back.
 */
@Entity
@Table(name = "pharmacy_store_return",
       uniqueConstraints = @UniqueConstraint(name = "uk_p2s_return_no", columnNames = "return_no"),
       indexes = {
               @Index(name = "idx_p2s_return_pharmacy",    columnList = "pharmacy_uid"),
               @Index(name = "idx_p2s_return_store",       columnList = "store_uid"),
               @Index(name = "idx_p2s_return_status",      columnList = "status"),
               @Index(name = "idx_p2s_return_return_date", columnList = "return_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyStoreReturn extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_no", nullable = false, length = 32)
    private String returnNo;

    @Column(name = "pharmacy_uid", nullable = false, length = 26) private String pharmacyUid;
    @Column(name = "store_uid",    nullable = false, length = 26) private String storeUid;

    @Column(name = "return_date", nullable = false) private LocalDate returnDate;

    @Setter @Column(length = 500) private String reason;
    @Setter @Column(length = 500) private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PharmacyStoreReturnStatus status = PharmacyStoreReturnStatus.DRAFT;

    @Setter @Column(name = "submitted_at")               private Instant submittedAt;
    @Setter @Column(name = "submitted_by_username", length = 64) private String submittedByUsername;
    @Setter @Column(name = "completed_at")               private Instant completedAt;
    @Setter @Column(name = "completed_by_username", length = 64) private String completedByUsername;
    @Setter @Column(name = "rejected_at")                private Instant rejectedAt;
    @Setter @Column(name = "rejected_by_username",  length = 64) private String rejectedByUsername;
    @Setter @Column(name = "reject_reason", length = 255)        private String rejectReason;
    @Setter @Column(name = "cancelled_at")               private Instant cancelledAt;

    public PharmacyStoreReturn(String returnNo, String pharmacyUid, String storeUid,
                               LocalDate returnDate, String reason, String note) {
        this.returnNo = returnNo;
        this.pharmacyUid = pharmacyUid;
        this.storeUid = storeUid;
        this.returnDate = returnDate == null ? LocalDate.now() : returnDate;
        this.reason = reason;
        this.note = note;
    }

    public boolean isEditable() {
        return status == PharmacyStoreReturnStatus.DRAFT;
    }

    public void submit(String username) {
        if (status != PharmacyStoreReturnStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT returns can be submitted (current: " + status + ")");
        }
        status = PharmacyStoreReturnStatus.SUBMITTED;
        submittedAt = Instant.now();
        submittedByUsername = username;
    }

    public void complete(String username) {
        if (status != PharmacyStoreReturnStatus.SUBMITTED) {
            throw new BusinessRuleException("Only SUBMITTED returns can be completed (current: " + status + ")");
        }
        status = PharmacyStoreReturnStatus.COMPLETED;
        completedAt = Instant.now();
        completedByUsername = username;
    }

    public void reject(String username, String reason) {
        if (status != PharmacyStoreReturnStatus.SUBMITTED) {
            throw new BusinessRuleException("Only SUBMITTED returns can be rejected (current: " + status + ")");
        }
        status = PharmacyStoreReturnStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectedByUsername = username;
        rejectReason = reason;
    }

    public void cancel() {
        if (status == PharmacyStoreReturnStatus.CANCELLED) return;
        if (status != PharmacyStoreReturnStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT returns can be cancelled (current: " + status + ")");
        }
        status = PharmacyStoreReturnStatus.CANCELLED;
        cancelledAt = Instant.now();
    }
}
