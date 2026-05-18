package com.otapp.hmis.engine.masterdata.dosingfrequency.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dosing-frequency lookup — BD (twice/day), TDS (three times/day), QID
 * (four times/day), STAT (once), PRN (as needed), etc. (PROCESS.md
 * §17.13).
 *
 * <p>{@link #timesPerDay} drives pharmacy quantity-per-day calculations:
 * a 7-day TDS prescription needs 7 × 3 = 21 doses. Null for "as-needed"
 * codes like PRN where the math doesn't apply.
 */
@Entity
@Table(name = "md_dosing_frequency",
       uniqueConstraints = @UniqueConstraint(name = "uk_md_dosing_freq_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DosingFrequency extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)  private String code;
    @Setter @Column(nullable = false, length = 120) private String name;
    /** Doses per 24h; null for as-needed codes (e.g. PRN). */
    @Setter @Column(name = "times_per_day")          private Integer timesPerDay;
    @Setter @Column(length = 500)                    private String description;
    @Setter @Column(nullable = false)                private boolean active = true;

    public DosingFrequency(String code, String name, Integer timesPerDay, String description) {
        this.code = code;
        this.name = name;
        this.timesPerDay = timesPerDay;
        this.description = description;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
