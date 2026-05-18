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
 * Transfer Order (TO) — pharmacy ↔ pharmacy variant. The delivering
 * pharmacy's commitment to ship a set of medicines / batches to the
 * requesting pharmacy. Created against an APPROVED+SUBMITTED RO.
 * Delivering-pharmacy stock is decremented (FEFO, TRANSFER_OUT) when
 * the TO reaches GOODS_ISSUED.
 */
@Entity
@Table(name = "pharmacy_to_pharmacy_to",
       uniqueConstraints = @UniqueConstraint(name = "uk_p2p_to_no", columnNames = "to_no"),
       indexes = {
               @Index(name = "idx_p2p_to_requester", columnList = "requesting_pharmacy_uid"),
               @Index(name = "idx_p2p_to_deliverer", columnList = "delivering_pharmacy_uid"),
               @Index(name = "idx_p2p_to_ro",        columnList = "ro_uid"),
               @Index(name = "idx_p2p_to_status",    columnList = "status"),
               @Index(name = "idx_p2p_to_order_date",columnList = "order_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyToPharmacyTO extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_no", nullable = false, length = 32)
    private String toNo;

    @Column(name = "ro_uid", nullable = false, length = 26) private String roUid;

    @Column(name = "requesting_pharmacy_uid", nullable = false, length = 26)
    private String requestingPharmacyUid;

    @Column(name = "delivering_pharmacy_uid", nullable = false, length = 26)
    private String deliveringPharmacyUid;

    @Column(name = "order_date", nullable = false) private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferDocStatus status = TransferDocStatus.PENDING;

    @Setter @Column(name = "verified_at")   private Instant verifiedAt;
    @Setter @Column(name = "approved_at")   private Instant approvedAt;
    @Setter @Column(name = "issued_at")     private Instant issuedAt;
    @Setter @Column(name = "completed_at")  private Instant completedAt;
    @Setter @Column(name = "rejected_at")   private Instant rejectedAt;
    @Setter @Column(name = "rejected_reason", length = 255) private String rejectedReason;
    @Setter @Column(name = "note",          length = 500) private String note;

    public PharmacyToPharmacyTO(String toNo, String roUid, String requestingPharmacyUid,
                                String deliveringPharmacyUid, LocalDate orderDate, String note) {
        this.toNo = toNo;
        this.roUid = roUid;
        this.requestingPharmacyUid = requestingPharmacyUid;
        this.deliveringPharmacyUid = deliveringPharmacyUid;
        this.orderDate = orderDate == null ? LocalDate.now() : orderDate;
        this.note = note;
    }

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

    public void markGoodsIssued() {
        requireStatus("issue goods", TransferDocStatus.APPROVED);
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
            throw new BusinessRuleException("Cannot reject a " + status + " TO");
        }
        if (status == TransferDocStatus.GOODS_ISSUED) {
            throw new BusinessRuleException("Cannot reject a TO whose goods have already shipped");
        }
        status = TransferDocStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectedReason = reason;
    }

    public boolean isTerminal() {
        return status == TransferDocStatus.COMPLETED || status == TransferDocStatus.REJECTED;
    }

    private void requireStatus(String op, TransferDocStatus... allowed) {
        for (TransferDocStatus s : allowed) {
            if (status == s) return;
        }
        throw new BusinessRuleException(
                "Cannot " + op + " TO from status " + status);
    }
}
