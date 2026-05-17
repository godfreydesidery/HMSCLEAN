package com.otapp.hmis.engine.store.stock.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Current on-hand quantity of a medicine at a central store. One row per
 * {@code (storeUid, medicineUid)}. Mutations go through
 * {@link #applyDelta(int)} so the invariant "balance &ge; 0" lives on
 * the aggregate.
 */
@Entity
@Table(name = "store_stock_balance",
       uniqueConstraints = @UniqueConstraint(name = "uk_store_stock_balance_store_medicine",
                                             columnNames = {"store_uid", "medicine_uid"}),
       indexes = {
               @Index(name = "idx_store_stock_balance_store",    columnList = "store_uid"),
               @Index(name = "idx_store_stock_balance_medicine", columnList = "medicine_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreStockBalance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_uid", nullable = false, length = 26)
    private String storeUid;

    @Column(name = "medicine_uid", nullable = false, length = 26)
    private String medicineUid;

    @Column(nullable = false)
    private int quantity;

    public StoreStockBalance(String storeUid, String medicineUid) {
        this.storeUid = storeUid;
        this.medicineUid = medicineUid;
        this.quantity = 0;
    }

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
