package com.otapp.hmis.engine.encounter.operative.domain;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Structured operative record for a PROCEDURE order (PROCESS.md §7,
 * §17.6). One per procedure; sibling of `OrderResult` — `OrderResult`
 * carries the narrative summary surfaced on the consultation, while
 * this entity carries the structured intra-op fields (findings,
 * technique, instruments, complications, surgical team, anaesthesia,
 * timing) that the legacy operative note documents.
 *
 * <p>Once {@link #lockedAt} is set the record is read-only — any
 * subsequent change must go through a separate amendment workflow (not
 * yet implemented; tracked in PROCESS.md follow-ups).
 */
@Entity
@Table(name = "operative_record",
       uniqueConstraints = @UniqueConstraint(name = "uk_operative_record_order", columnNames = "order_uid"),
       indexes = {
               @Index(name = "idx_operative_record_started",  columnList = "started_at"),
               @Index(name = "idx_operative_record_surgeon",  columnList = "surgeon_username")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperativeRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The PROCEDURE-kind ClinicalOrder this op note belongs to. 1:1. */
    @Column(name = "order_uid", nullable = false, length = 26)
    private String orderUid;

    // ----- structured clinical fields --------------------------------------

    @Setter @Column(length = 4000) private String findings;
    @Setter @Column(length = 4000) private String technique;
    @Setter @Column(length = 2000) private String instruments;
    @Setter @Column(length = 2000) private String complications;
    @Setter @Column(length = 2000) private String specimens;

    // ----- surgical team ---------------------------------------------------

    @Setter @Column(name = "surgeon_username",     length = 64) private String surgeonUsername;
    @Setter @Column(name = "assistants",           length = 500) private String assistants;
    @Setter @Column(name = "anaesthetist_username", length = 64) private String anaesthetistUsername;
    @Setter @Column(name = "anaesthesia_type",     length = 64) private String anaesthesiaType;
    @Setter @Column(name = "scrub_nurse",          length = 120) private String scrubNurse;
    @Setter @Column(name = "circulating_nurse",    length = 120) private String circulatingNurse;

    // ----- timing ----------------------------------------------------------

    @Setter @Column(name = "started_at") private Instant startedAt;
    @Setter @Column(name = "ended_at")   private Instant endedAt;

    // ----- lifecycle -------------------------------------------------------

    @Column(name = "authored_by_username", nullable = false, length = 64)
    private String authoredByUsername;
    @Column(name = "authored_at",          nullable = false)
    private Instant authoredAt;

    @Setter @Column(name = "locked_at")               private Instant lockedAt;
    @Setter @Column(name = "locked_by_username", length = 64) private String lockedByUsername;

    public OperativeRecord(String orderUid, String authoredByUsername) {
        this.orderUid = orderUid;
        this.authoredByUsername = authoredByUsername;
        this.authoredAt = Instant.now();
    }

    public boolean isLocked() {
        return lockedAt != null;
    }

    public void requireEditable() {
        if (isLocked()) {
            throw new BusinessRuleException(
                    "Operative record is locked (locked at " + lockedAt + "); use amend to change it");
        }
    }

    public void lock(String username) {
        if (isLocked()) return;
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw new BusinessRuleException("endedAt cannot be before startedAt");
        }
        this.lockedAt = Instant.now();
        this.lockedByUsername = username;
    }
}
