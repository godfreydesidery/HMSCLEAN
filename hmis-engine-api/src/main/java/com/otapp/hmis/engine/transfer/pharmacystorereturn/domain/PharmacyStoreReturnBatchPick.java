package com.otapp.hmis.engine.transfer.pharmacystorereturn.domain;

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

/**
 * Records that {@link #quantity} units came out of the source pharmacy's
 * batch {@link #sourceBatchUid} to satisfy {@link #returnLineUid}.
 * Persisted on COMPLETED — the FEFO walk at the pharmacy can pull from
 * multiple batches, and each batch's metadata is copied forward so the
 * store can credit the right (or a new) batch.
 */
@Entity
@Table(name = "pharmacy_store_return_batch_pick",
       indexes = {
               @Index(name = "idx_p2s_return_pick_line",  columnList = "return_line_uid"),
               @Index(name = "idx_p2s_return_pick_batch", columnList = "source_batch_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyStoreReturnBatchPick extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_line_uid",  nullable = false, length = 26) private String returnLineUid;
    @Column(name = "source_batch_uid", nullable = false, length = 26) private String sourceBatchUid;
    @Column(name = "batch_no",         nullable = false, length = 64) private String batchNo;
    @Column(name = "manufactured_date") private LocalDate manufacturedDate;
    @Column(name = "expires_at")       private LocalDate expiresAt;

    @Column(nullable = false) private int quantity;

    public PharmacyStoreReturnBatchPick(String returnLineUid, String sourceBatchUid,
                                        String batchNo, LocalDate manufacturedDate,
                                        LocalDate expiresAt, int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Pick quantity must be positive");
        }
        this.returnLineUid = returnLineUid;
        this.sourceBatchUid = sourceBatchUid;
        this.batchNo = batchNo;
        this.manufacturedDate = manufacturedDate;
        this.expiresAt = expiresAt;
        this.quantity = quantity;
    }
}
