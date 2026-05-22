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

/**
 * Append-only addendum to a locked {@link OperativeRecord}. Stores
 * just the fields the amender is changing, plus a required reason
 * explaining the correction (PROCESS.md follow-up to §7).
 *
 * <p>Amendments do not overwrite the original record — readers should
 * surface the locked op-note alongside its amendment history. Each
 * amendment is immutable once persisted.
 */
@Entity
@Table(name = "operative_record_amendment",
       uniqueConstraints = @UniqueConstraint(name = "uk_operative_record_amendment_seq",
                                             columnNames = {"operative_record_uid", "amendment_no"}),
       indexes = {
               @Index(name = "idx_op_record_amend_record",   columnList = "operative_record_uid"),
               @Index(name = "idx_op_record_amend_authored", columnList = "authored_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperativeRecordAmendment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operative_record_uid", nullable = false, length = 26)
    private String operativeRecordUid;

    /** Sequential number per record: 1, 2, 3 … */
    @Column(name = "amendment_no", nullable = false)
    private int amendmentNo;

    /** Required — why a locked op note is being amended. */
    @Column(nullable = false, length = 1000) private String reason;

    // Only the fields being changed are set on a given amendment; the rest
    // stay null. Readers compose "current state" as: original + applied
    // amendments in sequence.
    @Column(length = 4000)              private String findings;
    @Column(length = 4000)              private String technique;
    @Column(length = 2000)              private String instruments;
    @Column(length = 2000)              private String complications;
    @Column(length = 2000)              private String specimens;
    @Column(length = 500)               private String assistants;
    @Column(length = 64)                private String anaesthesiaType;
    @Column(length = 120)               private String scrubNurse;
    @Column(length = 120)               private String circulatingNurse;

    @Column(name = "authored_by_username", nullable = false, length = 64)
    private String authoredByUsername;
    @Column(name = "authored_at", nullable = false)
    private Instant authoredAt;

    public OperativeRecordAmendment(String operativeRecordUid, int amendmentNo,
                                    String reason, String authoredByUsername,
                                    String findings, String technique, String instruments,
                                    String complications, String specimens,
                                    String assistants, String anaesthesiaType,
                                    String scrubNurse, String circulatingNurse) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Amendment reason is required");
        }
        if (allBlank(findings, technique, instruments, complications, specimens,
                assistants, anaesthesiaType, scrubNurse, circulatingNurse)) {
            throw new BusinessRuleException("At least one field must be amended");
        }
        this.operativeRecordUid = operativeRecordUid;
        this.amendmentNo = amendmentNo;
        this.reason = reason;
        this.authoredByUsername = authoredByUsername;
        this.authoredAt = Instant.now();
        this.findings = findings;
        this.technique = technique;
        this.instruments = instruments;
        this.complications = complications;
        this.specimens = specimens;
        this.assistants = assistants;
        this.anaesthesiaType = anaesthesiaType;
        this.scrubNurse = scrubNurse;
        this.circulatingNurse = circulatingNurse;
    }

    private static boolean allBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return false;
        }
        return true;
    }
}
