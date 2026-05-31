package com.otapp.hmis.engine.store.stock.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single batch / lot of a medicine sitting in a central store. The
 * {@code (storeUid, medicineUid)} balance kept in {@link StoreStockBalance}
 * is the sum across the store's batches.
 *
 * <p>FEFO issue (First-Expired-First-Out) picks batches in
 * {@code expiresAt} ascending order; batches without an expiry sort last.
 */
@Entity
@Table(name = "store_stock_batch",
       uniqueConstraints = @UniqueConstraint(name = "uk_store_stock_batch_store_medicine_batchno",
                                             columnNames = {"store_uid", "medicine_uid", "batch_no"}),
       indexes = {
               @Index(name = "idx_store_stock_batch_store_medicine", columnList = "store_uid, medicine_uid"),
               @Index(name = "idx_store_stock_batch_expires",        columnList = "expires_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreStockBatch extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_uid", nullable = false, length = 26)
    private String storeUid;

    @Column(name = "medicine_uid", nullable = false, length = 26)
    private String medicineUid;

    /** Supplier-issued batch / lot number. Required for traceability. */
    @Column(name = "batch_no", nullable = false, length = 64)
    private String batchNo;

    /** Supplier-stated manufactured date for the batch (nullable). */
    @Setter @Column(name = "manufactured_date") private LocalDate manufacturedDate;

    @Setter @Column(name = "expires_at") private LocalDate expiresAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private int quantity;

    public StoreStockBatch(String storeUid, String medicineUid, String batchNo, LocalDate expiresAt) {
        this(storeUid, medicineUid, batchNo, null, expiresAt);
    }

    public StoreStockBatch(String storeUid, String medicineUid, String batchNo,
                           LocalDate manufacturedDate, LocalDate expiresAt) {
        this.storeUid = storeUid;
        this.medicineUid = medicineUid;
        this.batchNo = batchNo;
        this.manufacturedDate = manufacturedDate;
        this.expiresAt = expiresAt;
        this.receivedAt = Instant.now();
        this.quantity = 0;
    }

    /** Apply a signed delta — negative pushes towards zero, never below. */
    public void applyDelta(int delta) {
        if (delta == 0) {
            throw new BusinessRuleException("Batch delta must be non-zero");
        }
        long next = (long) quantity + delta;
        if (next < 0) {
            throw new BusinessRuleException(
                    "Insufficient batch stock: " + batchNo + " has " + quantity + ", requested " + (-delta));
        }
        if (next > Integer.MAX_VALUE) {
            throw new BusinessRuleException("Batch quantity overflow");
        }
        this.quantity = (int) next;
    }

    public boolean isExpired() {
        return expiresAt != null && !expiresAt.isAfter(LocalDate.now());
    }
}
