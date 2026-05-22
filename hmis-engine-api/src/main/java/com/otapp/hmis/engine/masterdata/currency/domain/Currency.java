package com.otapp.hmis.engine.masterdata.currency.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A currency the system may price and bill in (ISO-4217 three-letter code).
 * Exactly one row is the {@code isDefault} currency — used as the fallback
 * whenever a price or invoice has no explicit currency of its own.
 */
@Entity
@Table(name = "md_currency", uniqueConstraints = @UniqueConstraint(name = "uk_md_currency_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Currency extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String code;

    @Setter
    @Column(nullable = false, length = 80)
    private String name;

    @Setter
    @Column(length = 8)
    private String symbol;

    @Column(name = "is_default", nullable = false)
    private boolean defaultCurrency = false;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    public Currency(String code, String name, String symbol) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
    }

    public void activate() { this.active = true; }
    public void deactivate() { this.active = false; }

    public void markDefault() { this.defaultCurrency = true; }
    public void unmarkDefault() { this.defaultCurrency = false; }
}
