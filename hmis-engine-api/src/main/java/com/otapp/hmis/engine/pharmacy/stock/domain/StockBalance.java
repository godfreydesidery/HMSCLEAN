package com.otapp.hmis.engine.pharmacy.stock.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Current on-hand quantity of a medicine at a pharmacy. One row per
 * {@code (pharmacyUid, medicineUid)}. Mutations go through
 * {@link #applyDelta(int)} so the invariant "balance &ge; 0" lives on
 * the aggregate.
 */
@Entity
@Table(name = "pharmacy_stock_balance",
       uniqueConstraints = @UniqueConstraint(name = "uk_stock_balance_pharmacy_medicine",
                                             columnNames = {"pharmacy_uid", "medicine_uid"}),
       indexes = {
               @Index(name = "idx_stock_balance_pharmacy", columnList = "pharmacy_uid"),
               @Index(name = "idx_stock_balance_medicine", columnList = "medicine_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockBalance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pharmacy_uid", nullable = false, length = 26)
    private String pharmacyUid;

    @Column(name = "medicine_uid", nullable = false, length = 26)
    private String medicineUid;

    @Column(nullable = false)
    private int quantity;

    public StockBalance(String pharmacyUid, String medicineUid) {
        this.pharmacyUid = pharmacyUid;
        this.medicineUid = medicineUid;
        this.quantity = 0;
    }

    /**
     * Applies a positive or negative delta. Negative deltas may not push
     * the balance below zero; positive deltas are unbounded.
     */
    public void applyDelta(int delta) {
        if (delta == 0) {
            throw new BusinessRuleException("Stock delta must be non-zero");
        }
        long next = (long) quantity + delta;
        if (next < 0) {
            throw new BusinessRuleException(
                    "Insufficient stock: on hand " + quantity + ", requested " + (-delta));
        }
        if (next > Integer.MAX_VALUE) {
            throw new BusinessRuleException("Stock balance overflow");
        }
        this.quantity = (int) next;
    }
}
