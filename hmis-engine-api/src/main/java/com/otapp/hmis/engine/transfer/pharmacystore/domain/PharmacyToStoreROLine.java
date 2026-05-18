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
import lombok.Setter;

/**
 * One requested medicine on a Request Order. {@link #fulfilledQuantity}
 * accumulates as the corresponding TO is issued — it never exceeds
 * {@link #requestedQuantity}.
 */
@Entity
@Table(name = "pharmacy_to_store_ro_line",
       indexes = {
               @Index(name = "idx_p2s_ro_line_ro",       columnList = "ro_uid"),
               @Index(name = "idx_p2s_ro_line_medicine", columnList = "medicine_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyToStoreROLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ro_uid",       nullable = false, length = 26) private String roUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    /** Unit the requester typed in. Null = base unit (legacy lines). */
    @Column(name = "unit_uid", length = 26) private String unitUid;

    /** Always in BASE units. Display quantity = requestedQuantity / unit.factorToBase. */
    @Column(name = "requested_quantity", nullable = false) private int requestedQuantity;

    @Setter @Column(name = "fulfilled_quantity", nullable = false) private int fulfilledQuantity = 0;

    @Setter @Column(name = "note", length = 500) private String note;

    public PharmacyToStoreROLine(String roUid, String medicineUid, String unitUid,
                                 int requestedQuantity, String note) {
        if (requestedQuantity <= 0) {
            throw new BusinessRuleException("Requested quantity must be positive");
        }
        this.roUid = roUid;
        this.medicineUid = medicineUid;
        this.unitUid = unitUid;
        this.requestedQuantity = requestedQuantity;
        this.note = note;
    }

    public int outstandingQuantity() {
        return Math.max(0, requestedQuantity - fulfilledQuantity);
    }

    public void recordFulfilment(int delta) {
        if (delta <= 0) {
            throw new BusinessRuleException("Fulfilment delta must be positive");
        }
        int next = fulfilledQuantity + delta;
        if (next > requestedQuantity) {
            throw new BusinessRuleException(
                    "Fulfilment " + next + " exceeds requested " + requestedQuantity);
        }
        fulfilledQuantity = next;
    }
}
