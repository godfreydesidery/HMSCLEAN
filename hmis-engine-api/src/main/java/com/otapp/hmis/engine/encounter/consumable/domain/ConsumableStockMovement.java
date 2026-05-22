package com.otapp.hmis.engine.encounter.consumable.domain;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ledger row for a single consumable stock change. Quantity is signed:
 * positive for {@code RECEIPT} and positive {@code ADJUSTMENT}, negative
 * for {@code ISSUE_TO_WARD} and {@code WASTAGE}.
 */
@Entity
@Table(name = "consumable_stock_movement",
       indexes = {
               @Index(name = "idx_consumable_stock_movement_source",
                      columnList = "source_kind, source_uid, occurred_at"),
               @Index(name = "idx_consumable_stock_movement_consumable",
                      columnList = "consumable_uid"),
               @Index(name = "idx_consumable_stock_movement_reference",
                      columnList = "reference_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumableStockMovement extends AuditableEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConsumableMovementKind kind;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "reference_uid", length = 26)
    private String referenceUid;

    @Column(length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_username", length = 64)
    private String actorUsername;

    @SuppressWarnings("java:S107") // 9-arg ctor mirrors the row shape; no benefit from a builder
    public ConsumableStockMovement(ConsumableSourceKind sourceKind, String sourceUid,
                                   String consumableUid, ConsumableMovementKind kind,
                                   int quantity, int balanceAfter,
                                   String referenceUid, String note, String actorUsername) {
        this.sourceKind = sourceKind;
        this.sourceUid = sourceUid;
        this.consumableUid = consumableUid;
        this.kind = kind;
        this.quantity = quantity;
        this.balanceAfter = balanceAfter;
        this.referenceUid = referenceUid;
        this.note = note;
        this.actorUsername = actorUsername;
        this.occurredAt = Instant.now();
    }
}
