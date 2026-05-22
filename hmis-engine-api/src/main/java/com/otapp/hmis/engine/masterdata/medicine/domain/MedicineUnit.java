package com.otapp.hmis.engine.masterdata.medicine.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A unit of measure for a medicine — e.g. a tablet, a blister of 10
 * tablets, a carton of 100 tablets, a 10 ml vial, a 5 ml dose drawn from
 * that vial. Each medicine has exactly one {@link #base} unit; every
 * other unit defines its size relative to the base via
 * {@link #factorToBase}.
 *
 * <p>Stock balances and movements are always stored in the medicine's
 * base unit. Conversion happens at the API boundary: when a caller
 * receives, dispenses, or transfers stock in an alternate unit, the
 * service multiplies the typed quantity by {@code factorToBase} to get
 * the integer base-unit value that lands on the ledger.
 *
 * <p>Example: a medicine whose base unit is {@code TAB} (1 tablet) can
 * carry {@code BLISTER} (factor 10) and {@code BOX} (factor 100). A user
 * who receives 5 boxes is recorded as having received 500 tablets.
 */
@Entity
@Table(name = "md_medicine_unit",
       uniqueConstraints = {
               @UniqueConstraint(name = "uk_md_medicine_unit_medicine_code",
                                 columnNames = {"medicine_uid", "code"})
       },
       indexes = {
               @Index(name = "idx_md_medicine_unit_medicine", columnList = "medicine_uid"),
               @Index(name = "idx_md_medicine_unit_base",     columnList = "medicine_uid, base")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicineUnit extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medicine_uid", nullable = false, length = 26)
    private String medicineUid;

    /** Short code, e.g. {@code TAB}, {@code BLISTER}, {@code BOX}. Uppercase. */
    @Column(nullable = false, length = 16)
    private String code;

    @Setter @Column(nullable = false, length = 80) private String name;

    /** How many base units one of this unit holds. Always &gt; 0; the base unit's factor is 1. */
    @Setter @Column(name = "factor_to_base", nullable = false) private int factorToBase;

    /** Exactly one row per medicine has {@code base = true}. */
    @Column(nullable = false) private boolean base;

    @Setter @Column(nullable = false) private boolean active = true;

    public MedicineUnit(String medicineUid, String code, String name, int factorToBase, boolean base) {
        if (factorToBase <= 0) {
            throw new BusinessRuleException("factorToBase must be positive");
        }
        if (base && factorToBase != 1) {
            throw new BusinessRuleException("Base unit must have factorToBase = 1");
        }
        this.medicineUid = medicineUid;
        this.code = code.trim().toUpperCase();
        this.name = name;
        this.factorToBase = factorToBase;
        this.base = base;
    }

    /** Convert {@code quantityInThisUnit} to base units. */
    public long toBase(long quantityInThisUnit) {
        return quantityInThisUnit * (long) factorToBase;
    }
}
