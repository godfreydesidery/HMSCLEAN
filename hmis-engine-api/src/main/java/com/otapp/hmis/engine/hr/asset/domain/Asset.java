package com.otapp.hmis.engine.hr.asset.domain;

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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fixed asset on the hospital's register (PROCESS.md §12, §17.11) —
 * equipment, furniture, vehicles, etc. Not part of consumable inventory
 * (that's pharmacy / store) and not a clinical artifact.
 *
 * <p>Category + location are free-text for V1 — upgrade to masterdata
 * aggregates later if needed.
 */
@Entity
@Table(name = "hr_asset",
       uniqueConstraints = @UniqueConstraint(name = "uk_hr_asset_tag", columnNames = "tag"),
       indexes = {
               @Index(name = "idx_hr_asset_status",   columnList = "status"),
               @Index(name = "idx_hr_asset_category", columnList = "category"),
               @Index(name = "idx_hr_asset_location", columnList = "location")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Asset tag / barcode — the physical sticker on the equipment. Unique. */
    @Column(nullable = false, length = 64)
    private String tag;

    @Setter @Column(nullable = false, length = 160) private String name;
    @Setter @Column(length = 80)                    private String category;
    @Setter @Column(length = 120)                   private String location;
    @Setter @Column(length = 500)                   private String description;

    @Setter @Column(name = "serial_no",   length = 120) private String serialNo;
    @Setter @Column(name = "manufacturer", length = 120) private String manufacturer;
    @Setter @Column(length = 120)                       private String model;

    @Setter @Column(name = "acquisition_date") private LocalDate acquisitionDate;
    @Setter @Column(name = "acquisition_cost", precision = 14, scale = 2) private BigDecimal acquisitionCost;
    @Setter @Column(length = 3)                              private String currency;

    /** Username (iam.User) of the person currently accountable for the asset. */
    @Setter @Column(name = "custodian_username", length = 64) private String custodianUsername;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssetStatus status = AssetStatus.ACTIVE;

    @Setter @Column(name = "retired_at")             private LocalDate retiredAt;
    @Setter @Column(name = "retired_reason", length = 500) private String retiredReason;

    public Asset(String tag, String name) {
        if (tag == null || tag.isBlank())   throw new BusinessRuleException("tag is required");
        if (name == null || name.isBlank()) throw new BusinessRuleException("name is required");
        this.tag = tag.trim();
        this.name = name.trim();
    }

    /** Take the asset out of active service (RETIRED / DISPOSED / LOST). Idempotent at a given target. */
    public void retire(AssetStatus target, LocalDate date, String reason) {
        if (target == null || target == AssetStatus.ACTIVE) {
            throw new BusinessRuleException("Retire target must be RETIRED, DISPOSED or LOST");
        }
        if (status == target) return;
        if (status == AssetStatus.DISPOSED) {
            throw new BusinessRuleException("Disposed assets cannot transition further");
        }
        if (status == AssetStatus.LOST && target != AssetStatus.DISPOSED) {
            throw new BusinessRuleException("LOST assets can only be moved to DISPOSED");
        }
        if (date == null) throw new BusinessRuleException("Retirement date is required");
        this.status = target;
        this.retiredAt = date;
        this.retiredReason = reason;
    }

    /** Return a RETIRED asset to active service (e.g. repaired). */
    public void reinstate() {
        if (status != AssetStatus.RETIRED) {
            throw new BusinessRuleException("Only RETIRED assets can be reinstated (current: " + status + ")");
        }
        this.status = AssetStatus.ACTIVE;
        this.retiredAt = null;
        this.retiredReason = null;
    }
}
