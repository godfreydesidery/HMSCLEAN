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
 * Receive Note (RN): the pharmacy's confirmation that the goods on a TO
 * arrived. Creating an RN drives the pharmacy-side stock increment per
 * batch picked, and completes both the TO and the parent RO.
 */
@Entity
@Table(name = "store_to_pharmacy_rn",
       uniqueConstraints = @UniqueConstraint(name = "uk_store_to_pharmacy_rn_no", columnNames = "rn_no"),
       indexes = {
               @Index(name = "idx_s2p_rn_pharmacy",      columnList = "pharmacy_uid"),
               @Index(name = "idx_s2p_rn_store",         columnList = "store_uid"),
               @Index(name = "idx_s2p_rn_to",            columnList = "to_uid"),
               @Index(name = "idx_s2p_rn_status",        columnList = "status"),
               @Index(name = "idx_s2p_rn_receiving_date",columnList = "receiving_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreToPharmacyRN extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rn_no", nullable = false, length = 32)
    private String rnNo;

    @Column(name = "to_uid",       nullable = false, length = 26) private String toUid;
    @Column(name = "pharmacy_uid", nullable = false, length = 26) private String pharmacyUid;
    @Column(name = "store_uid",    nullable = false, length = 26) private String storeUid;

    @Column(name = "receiving_date", nullable = false) private LocalDate receivingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReceiveNoteStatus status = ReceiveNoteStatus.PENDING;

    @Setter @Column(name = "completed_at") private Instant completedAt;
    @Setter @Column(name = "cancelled_at") private Instant cancelledAt;
    @Setter @Column(name = "note",  length = 500) private String note;

    public StoreToPharmacyRN(String rnNo, String toUid, String pharmacyUid,
                             String storeUid, LocalDate receivingDate, String note) {
        this.rnNo = rnNo;
        this.toUid = toUid;
        this.pharmacyUid = pharmacyUid;
        this.storeUid = storeUid;
        this.receivingDate = receivingDate == null ? LocalDate.now() : receivingDate;
        this.note = note;
    }

    public void markCompleted() {
        if (status != ReceiveNoteStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING RNs can be completed (current: " + status + ")");
        }
        status = ReceiveNoteStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void cancel() {
        if (status == ReceiveNoteStatus.COMPLETED) {
            throw new BusinessRuleException("Completed RNs cannot be cancelled");
        }
        status = ReceiveNoteStatus.CANCELLED;
        cancelledAt = Instant.now();
    }
}
