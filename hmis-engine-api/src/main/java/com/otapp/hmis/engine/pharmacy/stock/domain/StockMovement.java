package com.otapp.hmis.engine.pharmacy.stock.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ledger entry of one stock change at one pharmacy. Quantity is signed:
 * positive for additions ({@link StockMovementKind#RECEIPT}, {@code TRANSFER_IN},
 * positive adjustments), negative for removals.
 */
@Entity
@Table(name = "pharmacy_stock_movement",
       indexes = {
               @Index(name = "idx_stock_movement_pharmacy", columnList = "pharmacy_uid, occurred_at"),
               @Index(name = "idx_stock_movement_medicine", columnList = "medicine_uid"),
               @Index(name = "idx_stock_movement_kind",     columnList = "kind"),
               @Index(name = "idx_stock_movement_reference",columnList = "reference_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pharmacy_uid", nullable = false, length = 26) private String pharmacyUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StockMovementKind kind;

    /** Signed: + adds to balance, - removes. */
    @Column(nullable = false)
    private int quantity;

    /** Balance after the movement was applied, for fast stock-card display. */
    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    /** uid of the source document (e.g. prescription uid for a DISPENSE). */
    @Column(name = "reference_uid", length = 26)
    private String referenceUid;

    @Column(length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_username", length = 64)
    private String actorUsername;

    public StockMovement(String pharmacyUid, String medicineUid, StockMovementKind kind,
                         int quantity, int balanceAfter, String referenceUid,
                         String note, String actorUsername) {
        this.pharmacyUid = pharmacyUid;
        this.medicineUid = medicineUid;
        this.kind = kind;
        this.quantity = quantity;
        this.balanceAfter = balanceAfter;
        this.referenceUid = referenceUid;
        this.note = note;
        this.actorUsername = actorUsername;
        this.occurredAt = Instant.now();
    }
}
