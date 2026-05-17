package com.otapp.hmis.engine.store.stock.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ledger entry of one stock change at one store. Quantity is signed:
 * positive for additions ({@link StoreStockMovementKind#RECEIPT},
 * {@code RETURN}, positive adjustments), negative for removals
 * ({@code ISSUE}, {@code DIRECT_ISSUE}, {@code WASTAGE}).
 */
@Entity
@Table(name = "store_stock_movement",
       indexes = {
               @Index(name = "idx_store_stock_movement_store",    columnList = "store_uid, occurred_at"),
               @Index(name = "idx_store_stock_movement_medicine", columnList = "medicine_uid"),
               @Index(name = "idx_store_stock_movement_kind",     columnList = "kind"),
               @Index(name = "idx_store_stock_movement_reference",columnList = "reference_uid"),
               @Index(name = "idx_store_stock_movement_batch",    columnList = "batch_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreStockMovement extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_uid",    nullable = false, length = 26) private String storeUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StoreStockMovementKind kind;

    /** Signed: + adds to balance, - removes. */
    @Column(nullable = false)
    private int quantity;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    /** uid of the source document (e.g. goods receipt uid for a RECEIPT). */
    @Column(name = "reference_uid", length = 26)
    private String referenceUid;

    @Column(name = "batch_uid", length = 26)
    private String batchUid;

    @Column(name = "batch_no", length = 64)
    private String batchNo;

    @Column(length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_username", length = 64)
    private String actorUsername;

    public StoreStockMovement(String storeUid, String medicineUid, StoreStockMovementKind kind,
                              int quantity, int balanceAfter, String referenceUid,
                              String batchUid, String batchNo,
                              String note, String actorUsername) {
        this.storeUid = storeUid;
        this.medicineUid = medicineUid;
        this.kind = kind;
        this.quantity = quantity;
        this.balanceAfter = balanceAfter;
        this.referenceUid = referenceUid;
        this.batchUid = batchUid;
        this.batchNo = batchNo;
        this.note = note;
        this.actorUsername = actorUsername;
        this.occurredAt = Instant.now();
    }
}
