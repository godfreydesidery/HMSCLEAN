package com.otapp.hmis.engine.masterdata.pricing.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single priced cell in the pricing matrix.
 *
 * <p>{@code planUid} is {@code null} for the public cash price; otherwise it
 * points at an {@code InsurancePlan}. {@code (planUid, kind, serviceUid)} is
 * unique — there is at most one price per (plan, service) combination.
 */
@Entity
@Table(name = "md_service_price",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_md_service_price",
               columnNames = {"plan_uid", "kind", "service_uid", "currency"}),
       indexes = {
               @Index(name = "idx_md_service_price_service", columnList = "kind,service_uid"),
               @Index(name = "idx_md_service_price_plan",    columnList = "plan_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServicePrice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null means this row is the cash / public price. */
    @Column(name = "plan_uid", length = 26)
    private String planUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceKind kind;

    @Column(name = "service_uid", nullable = false, length = 26)
    private String serviceUid;

    @Setter
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Optional negotiable floor — a per-line override may not go below this. */
    @Setter
    @Column(name = "min_amount", precision = 14, scale = 2)
    private BigDecimal minAmount;

    /** Optional negotiable ceiling — a per-line override may not exceed this. */
    @Setter
    @Column(name = "max_amount", precision = 14, scale = 2)
    private BigDecimal maxAmount;

    @Setter
    @Column(nullable = false, length = 3)
    private String currency;

    @Setter @Column(length = 255) private String note;

    public ServicePrice(String planUid, ServiceKind kind, String serviceUid,
                        BigDecimal amount, String currency, String note) {
        this.planUid = planUid;
        this.kind = kind;
        this.serviceUid = serviceUid;
        this.amount = amount;
        this.currency = currency;
        this.note = note;
    }
}
