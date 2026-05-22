package com.otapp.hmis.engine.transfer.pharmacystore.domain;

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

/**
 * One medicine line on a Receive Note. {@link #receivedQuantity} may be
 * less than {@link #issuedQuantity} (the matching TO line's issued amount)
 * to record transit shortfalls.
 */
@Entity
@Table(name = "store_to_pharmacy_rn_line",
       indexes = {
               @Index(name = "idx_s2p_rn_line_rn",       columnList = "rn_uid"),
               @Index(name = "idx_s2p_rn_line_to_line",  columnList = "to_line_uid"),
               @Index(name = "idx_s2p_rn_line_medicine", columnList = "medicine_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreToPharmacyRNLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rn_uid",       nullable = false, length = 26) private String rnUid;
    @Column(name = "to_line_uid",  nullable = false, length = 26) private String toLineUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    /** Inherited from the source TO line. Null = base unit. */
    @Column(name = "unit_uid", length = 26) private String unitUid;

    @Column(name = "issued_quantity",   nullable = false) private int issuedQuantity;
    @Column(name = "received_quantity", nullable = false) private int receivedQuantity;

    public StoreToPharmacyRNLine(String rnUid, String toLineUid, String medicineUid,
                                 String unitUid, int issuedQuantity, int receivedQuantity) {
        if (issuedQuantity <= 0) {
            throw new BusinessRuleException("Issued quantity must be positive");
        }
        if (receivedQuantity < 0 || receivedQuantity > issuedQuantity) {
            throw new BusinessRuleException(
                    "Received qty " + receivedQuantity + " must be in [0, " + issuedQuantity + "]");
        }
        this.rnUid = rnUid;
        this.toLineUid = toLineUid;
        this.medicineUid = medicineUid;
        this.unitUid = unitUid;
        this.issuedQuantity = issuedQuantity;
        this.receivedQuantity = receivedQuantity;
    }

    public int shortfall() {
        return issuedQuantity - receivedQuantity;
    }
}
