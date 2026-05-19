package com.otapp.hmis.engine.encounter.consumable.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Integer stock balance for a consumable at one source location. Consumables
 * are tracked as a single quantity per (sourceKind, sourceUid, consumableUid)
 * — no batches or expiry, matching how warehouses actually track gauze /
 * saline / dressings in practice. Concurrent decrements are serialised by a
 * pessimistic-write lock at the repository.
 */
@Entity
@Table(name = "consumable_stock_balance",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_consumable_stock_balance",
               columnNames = {"source_kind", "source_uid", "consumable_uid"}),
       indexes = @Index(name = "idx_consumable_stock_balance_source",
                        columnList = "source_kind, source_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumableStockBalance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 16)
    private ConsumableSourceKind sourceKind;

    @Column(name = "source_uid", nullable = false, length = 26)
    private String sourceUid;

    @Column(name = "consumable_uid", nullable = false, length = 26)
    private String consumableUid;

    @Column(nullable = false)
    private int quantity;

    public ConsumableStockBalance(ConsumableSourceKind sourceKind, String sourceUid, String consumableUid) {
        this.sourceKind = sourceKind;
        this.sourceUid = sourceUid;
        this.consumableUid = consumableUid;
        this.quantity = 0;
    }

    /** Apply a signed delta. Never goes negative. */
    public void applyDelta(int delta) {
        if (delta == 0) {
            throw new BusinessRuleException("Consumable stock delta must be non-zero");
        }
        long next = (long) quantity + delta;
        if (next < 0) {
            throw new BusinessRuleException(
                    "Insufficient consumable stock: on hand " + quantity + ", requested " + (-delta));
        }
        if (next > Integer.MAX_VALUE) {
            throw new BusinessRuleException("Consumable stock quantity overflow");
        }
        this.quantity = (int) next;
    }
}
