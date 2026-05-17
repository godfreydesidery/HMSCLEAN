package com.otapp.hmis.engine.encounter.progressnote.domain;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One progress entry recorded during an admission. Notes are immutable
 * once recorded except for a soft delete (retained for audit).
 */
@Entity
@Table(name = "admission_progress_note",
       indexes = {
               @Index(name = "idx_progress_note_admission", columnList = "admission_uid, recorded_at"),
               @Index(name = "idx_progress_note_kind",      columnList = "kind")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgressNote extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid", nullable = false, length = 26)
    private String admissionUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProgressNoteKind kind;

    @Column(name = "author_username", nullable = false, length = 64)
    private String authorUsername;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 8000)
    private String body;

    @Setter @Column(name = "deleted_at") private Instant deletedAt;
    @Setter @Column(name = "deleted_by", length = 80) private String deletedBy;
    @Setter @Column(name = "deleted_reason", length = 255) private String deletedReason;

    public ProgressNote(String admissionUid, ProgressNoteKind kind, String authorUsername, String body) {
        this.admissionUid = admissionUid;
        this.kind = kind;
        this.authorUsername = authorUsername;
        this.body = body;
        this.recordedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(String username, String reason) {
        if (isDeleted()) {
            throw new BusinessRuleException("Note is already deleted");
        }
        this.deletedAt = Instant.now();
        this.deletedBy = username;
        this.deletedReason = reason;
    }
}
