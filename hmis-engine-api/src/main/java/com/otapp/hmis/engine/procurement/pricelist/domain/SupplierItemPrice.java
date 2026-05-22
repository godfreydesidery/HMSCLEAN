package com.otapp.hmis.engine.procurement.pricelist.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One supplier's quoted price for one medicine, valid over a date range
 * (PROCESS.md §10, §17.9). A supplier may carry many quotes for the
 * same medicine across time — the rolled-forward "current best" view
 * lives in the application service, not in any single row.
 *
 * <p>{@link #validFrom} is required; {@link #validTo} is open-ended
 * (null = no expiry). {@link #active} is a soft on/off independent of
 * the date range — useful for parking a quote without losing the
 * history.
 */
@Entity
@Table(name = "supplier_item_price",
       indexes = {
               @Index(name = "idx_supplier_item_price_supplier",  columnList = "supplier_uid"),
               @Index(name = "idx_supplier_item_price_medicine",  columnList = "medicine_uid"),
               @Index(name = "idx_supplier_item_price_lookup",
                      columnList = "supplier_uid, medicine_uid, active, valid_from"),
               @Index(name = "idx_supplier_item_price_valid_from", columnList = "valid_from")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierItemPrice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_uid", nullable = false, length = 26) private String supplierUid;
    @Column(name = "medicine_uid", nullable = false, length = 26) private String medicineUid;

    @Setter @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Setter @Column(nullable = false, length = 3)
    private String currency = "TZS";

    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Setter @Column(name = "valid_to")             private LocalDate validTo;

    @Setter @Column(nullable = false)              private boolean active = true;

    @Setter @Column(length = 500)                  private String notes;

    public SupplierItemPrice(String supplierUid, String medicineUid,
                             BigDecimal unitPrice, String currency,
                             LocalDate validFrom, LocalDate validTo, String notes) {
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new BusinessRuleException("unitPrice must be positive");
        }
        if (validFrom == null) {
            throw new BusinessRuleException("validFrom is required");
        }
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new BusinessRuleException("validTo cannot be before validFrom");
        }
        this.supplierUid = supplierUid;
        this.medicineUid = medicineUid;
        this.unitPrice = unitPrice;
        this.currency = (currency == null || currency.isBlank()) ? "TZS" : currency.toUpperCase();
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.notes = notes;
    }

    /** True if the quote is currently usable: active AND today within the window. */
    public boolean isCurrentlyValid() {
        if (!active) return false;
        LocalDate today = LocalDate.now();
        if (today.isBefore(validFrom)) return false;
        return validTo == null || !today.isAfter(validTo);
    }
}
