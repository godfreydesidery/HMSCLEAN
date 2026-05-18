package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

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
 * One medicine line on a P↔P Transfer Order. {@link #issuedQuantity} is
 * the sum of the picks recorded against this line (see
 * {@link PharmacyToPharmacyTOBatchPick}); it caps at
 * {@link #requestedQuantity}.
 */
@Entity
@Table(name = "pharmacy_to_pharmacy_to_line",
       indexes = {
               @Index(name = "idx_p2p_to_line_to",       columnList = "to_uid"),
               @Index(name = "idx_p2p_to_line_ro_line",  columnList = "ro_line_uid"),
               @Index(name = "idx_p2p_to_line_medicine", columnList = "medicine_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PharmacyToPharmacyTOLine extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_uid",       nullable = false, length = 26) private String toUid;
    @Column(name = "ro_line_uid",  nullable = false, length = 26) private String roLineUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    /** Inherited from the source RO line. Null = base unit. */
    @Column(name = "unit_uid", length = 26) private String unitUid;

    @Column(name = "requested_quantity", nullable = false) private int requestedQuantity;
    @Setter @Column(name = "issued_quantity",  nullable = false) private int issuedQuantity = 0;
    @Setter @Column(name = "received_quantity", nullable = false) private int receivedQuantity = 0;

    public PharmacyToPharmacyTOLine(String toUid, String roLineUid, String medicineUid,
                                    String unitUid, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new BusinessRuleException("Requested quantity must be positive");
        }
        this.toUid = toUid;
        this.roLineUid = roLineUid;
        this.medicineUid = medicineUid;
        this.unitUid = unitUid;
        this.requestedQuantity = requestedQuantity;
    }

    public void recordIssue(int delta) {
        if (delta <= 0) {
            throw new BusinessRuleException("Issued delta must be positive");
        }
        int next = issuedQuantity + delta;
        if (next > requestedQuantity) {
            throw new BusinessRuleException(
                    "Issued " + next + " exceeds requested " + requestedQuantity);
        }
        issuedQuantity = next;
    }

    public void recordReceipt(int delta) {
        if (delta <= 0) {
            throw new BusinessRuleException("Received delta must be positive");
        }
        int next = receivedQuantity + delta;
        if (next > issuedQuantity) {
            throw new BusinessRuleException(
                    "Received " + next + " exceeds issued " + issuedQuantity
                            + " — transit losses must be recorded as wastage on the receiving pharmacy");
        }
        receivedQuantity = next;
    }
}
