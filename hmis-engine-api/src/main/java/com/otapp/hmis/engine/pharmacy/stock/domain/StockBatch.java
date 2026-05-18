package com.otapp.hmis.engine.pharmacy.stock.domain;

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
 * A single batch / lot of a medicine sitting in a pharmacy. Multiple
 * batches of the same medicine can coexist in the same pharmacy (different
 * batch numbers, different expiry dates). The {@code (pharmacyUid,
 * medicineUid)} balance kept in {@link StockBalance} is the sum across the
 * pharmacy's batches.
 *
 * <p>FEFO dispensing (First-Expired-First-Out) picks batches in
 * {@code expiresAt} ascending order; batches without an expiry sort last.
 */
@Entity
@Table(name = "pharmacy_stock_batch",
       uniqueConstraints = @UniqueConstraint(name = "uk_stock_batch_pharmacy_medicine_batchno",
                                             columnNames = {"pharmacy_uid", "medicine_uid", "batch_no"}),
       indexes = {
               @Index(name = "idx_stock_batch_pharmacy_medicine", columnList = "pharmacy_uid, medicine_uid"),
               @Index(name = "idx_stock_batch_expires",           columnList = "expires_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockBatch extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pharmacy_uid", nullable = false, length = 26)
    private String pharmacyUid;

    @Column(name = "medicine_uid", nullable = false, length = 26)
    private String medicineUid;

    /** Supplier-issued batch / lot number. Required for traceability. */
    @Column(name = "batch_no", nullable = false, length = 64)
    private String batchNo;

    @Setter @Column(name = "expires_at") private LocalDate expiresAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private int quantity;

    public StockBatch(String pharmacyUid, String medicineUid, String batchNo, LocalDate expiresAt) {
        this.pharmacyUid = pharmacyUid;
        this.medicineUid = medicineUid;
        this.batchNo = batchNo;
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
