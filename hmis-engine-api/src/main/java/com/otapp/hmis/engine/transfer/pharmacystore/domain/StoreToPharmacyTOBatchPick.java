package com.otapp.hmis.engine.transfer.pharmacystore.domain;

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
 * Records that {@link #quantity} units came out of store batch
 * {@link #sourceBatchUid} to satisfy {@link #toLineUid}. A TO line may have
 * several picks (FEFO walked across multiple batches). Batch metadata is
 * snapshotted here so that the receiving pharmacy can create a matching
 * StockBatch on the pharmacy side without re-querying the source.
 */
@Entity
@Table(name = "store_to_pharmacy_to_batch_pick",
       indexes = {
               @Index(name = "idx_s2p_to_pick_line",   columnList = "to_line_uid"),
               @Index(name = "idx_s2p_to_pick_batch",  columnList = "source_batch_uid"),
               @Index(name = "idx_s2p_to_pick_rn_line",columnList = "rn_line_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreToPharmacyTOBatchPick extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_line_uid",      nullable = false, length = 26) private String toLineUid;
    @Column(name = "source_batch_uid", nullable = false, length = 26) private String sourceBatchUid;
    @Column(name = "batch_no",         nullable = false, length = 64) private String batchNo;
    @Column(name = "manufactured_date") private LocalDate manufacturedDate;
    @Column(name = "expires_at")       private LocalDate expiresAt;

    @Column(nullable = false) private int quantity;

    /**
     * Set by the RN: links the pick to the RN line that confirmed receipt.
     * Stays null until the pharmacy signs the RN.
     */
    @Setter @Column(name = "rn_line_uid", length = 26) private String rnLineUid;

    public StoreToPharmacyTOBatchPick(String toLineUid, String sourceBatchUid,
                                      String batchNo, LocalDate manufacturedDate,
                                      LocalDate expiresAt, int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Pick quantity must be positive");
        }
        this.toLineUid = toLineUid;
        this.sourceBatchUid = sourceBatchUid;
        this.batchNo = batchNo;
        this.manufacturedDate = manufacturedDate;
        this.expiresAt = expiresAt;
        this.quantity = quantity;
    }
}
