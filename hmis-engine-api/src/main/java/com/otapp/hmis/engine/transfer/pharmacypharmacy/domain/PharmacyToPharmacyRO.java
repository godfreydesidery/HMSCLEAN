package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
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
 * Request Order (RO) — pharmacy ↔ pharmacy variant. The requesting
 * pharmacy asks the delivering pharmacy for stock; once APPROVED +
 * SUBMITTED, the delivering pharmacy can fulfil with a
 * {@link PharmacyToPharmacyTO}. Same lifecycle as the P↔S RO
 * (PROCESS.md §8.4, §15).
 */
@Entity
@Table(name = "pharmacy_to_pharmacy_ro",
       uniqueConstraints = @UniqueConstraint(name = "uk_p2p_ro_no", columnNames = "ro_no"),
       indexes = {
               @Index(name = "idx_p2p_ro_requester", columnList = "requesting_pharmacy_uid"),
               @Index(name = "idx_p2p_ro_deliverer", columnList = "delivering_pharmacy_uid"),
               @Index(name = "idx_p2p_ro_status",    columnList = "status"),
               @Index(name = "idx_p2p_ro_order_date",columnList = "order_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyToPharmacyRO extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable RO number, e.g. P2P-RO-2026-000123. */
    @Column(name = "ro_no", nullable = false, length = 32)
    private String roNo;

    @Column(name = "requesting_pharmacy_uid", nullable = false, length = 26)
    private String requestingPharmacyUid;

    @Column(name = "delivering_pharmacy_uid", nullable = false, length = 26)
    private String deliveringPharmacyUid;

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

    public PharmacyToPharmacyRO(String roNo, String requestingPharmacyUid, String deliveringPharmacyUid,
                                LocalDate orderDate, LocalDate validUntil, String note) {
        if (requestingPharmacyUid.equals(deliveringPharmacyUid)) {
            throw new BusinessRuleException("Requesting and delivering pharmacy must differ");
        }
        this.roNo = roNo;
        this.requestingPharmacyUid = requestingPharmacyUid;
        this.deliveringPharmacyUid = deliveringPharmacyUid;
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

    public void markInProcess() {
        if (status == TransferDocStatus.IN_PROCESS) return;
        requireStatus("start processing", TransferDocStatus.SUBMITTED);
        status = TransferDocStatus.IN_PROCESS;
        inProcessAt = Instant.now();
    }

    public void markGoodsIssued() {
        if (status == TransferDocStatus.GOODS_ISSUED) return;
        requireStatus("mark goods issued",
                TransferDocStatus.SUBMITTED, TransferDocStatus.IN_PROCESS);
        status = TransferDocStatus.GOODS_ISSUED;
        issuedAt = Instant.now();
    }

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

    private void requireStatus(String op, TransferDocStatus... allowed) {
        for (TransferDocStatus s : allowed) {
            if (status == s) return;
        }
        throw new BusinessRuleException(
                "Cannot " + op + " RO from status " + status);
    }
}
