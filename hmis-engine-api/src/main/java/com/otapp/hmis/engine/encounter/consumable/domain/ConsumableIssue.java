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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A nursing consumable issued to an inpatient during their stay — gauze,
 * IV fluid bag, syringe, dressing kit, etc. Drives the patient consumable
 * chart and feeds the admission invoice as a {@code CONSUMABLE} line.
 *
 * <p>The actual stock decrement at the source store / pharmacy is a
 * separate concern handled outside this aggregate; this row records the
 * audit fact that a consumable was used on the admission together with
 * the snapshot unit cost for billing.
 */
@Entity
@Table(name = "consumable_issue",
       indexes = {
               @Index(name = "idx_consumable_issue_admission", columnList = "admission_uid, issued_at"),
               @Index(name = "idx_consumable_issue_consumable", columnList = "consumable_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumableIssue extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid",  nullable = false, length = 26) private String admissionUid;
    @Column(name = "consumable_uid", nullable = false, length = 26) private String consumableUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 16)
    private ConsumableSourceKind sourceKind;

    /** uid of the source store or pharmacy the consumable was pulled from. */
    @Column(name = "source_location_uid", nullable = false, length = 26)
    private String sourceLocationUid;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "issued_by_username", nullable = false, length = 64)
    private String issuedByUsername;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Setter @Column(length = 500) private String note;

    @SuppressWarnings("java:S107") // 8-arg ctor mirrors the row shape; no benefit from a builder
    public ConsumableIssue(String admissionUid, String consumableUid,
                           ConsumableSourceKind sourceKind, String sourceLocationUid,
                           int quantity, BigDecimal unitCost,
                           String issuedByUsername, String note) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Consumable issue quantity must be positive");
        }
        if (unitCost == null || unitCost.signum() < 0) {
            throw new BusinessRuleException("Consumable unit cost must be non-negative");
        }
        this.admissionUid = admissionUid;
        this.consumableUid = consumableUid;
        this.sourceKind = sourceKind;
        this.sourceLocationUid = sourceLocationUid;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.issuedByUsername = issuedByUsername;
        this.issuedAt = Instant.now();
        this.note = note;
    }

    public BigDecimal lineAmount() {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }
}
