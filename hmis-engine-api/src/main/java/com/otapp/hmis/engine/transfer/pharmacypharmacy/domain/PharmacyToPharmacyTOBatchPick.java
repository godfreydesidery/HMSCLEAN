package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Records that {@link #quantity} units came out of the delivering
 * pharmacy's batch {@link #sourceBatchUid} to satisfy {@link #toLineUid}.
 * Batch metadata is snapshotted here so the receiving pharmacy can create
 * a matching StockBatch without re-querying the source.
 */
@Entity
@Table(name = "pharmacy_to_pharmacy_to_batch_pick",
       indexes = {
               @Index(name = "idx_p2p_to_pick_line",    columnList = "to_line_uid"),
               @Index(name = "idx_p2p_to_pick_batch",   columnList = "source_batch_uid"),
               @Index(name = "idx_p2p_to_pick_rn_line", columnList = "rn_line_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyToPharmacyTOBatchPick extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_line_uid",      nullable = false, length = 26) private String toLineUid;
    @Column(name = "source_batch_uid", nullable = false, length = 26) private String sourceBatchUid;
    @Column(name = "batch_no",         nullable = false, length = 64) private String batchNo;
    @Column(name = "expires_at")       private LocalDate expiresAt;

    @Column(nullable = false) private int quantity;

    @Setter @Column(name = "rn_line_uid", length = 26) private String rnLineUid;

    public PharmacyToPharmacyTOBatchPick(String toLineUid, String sourceBatchUid,
                                         String batchNo, LocalDate expiresAt, int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Pick quantity must be positive");
        }
        this.toLineUid = toLineUid;
        this.sourceBatchUid = sourceBatchUid;
        this.batchNo = batchNo;
        this.expiresAt = expiresAt;
        this.quantity = quantity;
    }
}
