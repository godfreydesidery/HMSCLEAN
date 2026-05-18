package com.otapp.hmis.engine.transfer.pharmacystorereturn.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line on a pharmacy-to-store return. Quantity stored in BASE units;
 * if the user typed in an alternate unit, the service multiplies via
 * {@link com.otapp.hmis.engine.masterdata.medicine.application.UnitConversionService}
 * before persisting. Display unit captured via {@link #unitUid}.
 */
@Entity
@Table(name = "pharmacy_store_return_line",
       indexes = {
               @Index(name = "idx_p2s_return_line_return",   columnList = "return_uid"),
               @Index(name = "idx_p2s_return_line_medicine", columnList = "medicine_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyStoreReturnLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_uid",   nullable = false, length = 26) private String returnUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    /** Display unit the user typed in. Null = base unit. */
    @Setter @Column(name = "unit_uid", length = 26) private String unitUid;

    /** Always in BASE units. */
    @Column(nullable = false) private int quantity;

    @Setter @Column(length = 500) private String reason;

    public PharmacyStoreReturnLine(String returnUid, String medicineUid, String unitUid,
                                   int quantity, String reason) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Return quantity must be positive");
        }
        this.returnUid = returnUid;
        this.medicineUid = medicineUid;
        this.unitUid = unitUid;
        this.quantity = quantity;
        this.reason = reason;
    }
}
