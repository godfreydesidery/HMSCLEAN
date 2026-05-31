package com.otapp.hmis.engine.common.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for persistent entities that need auditing.
 *
 * <p>Every entity has a numeric {@code id} and an externally-facing {@code uid}
 * (Crockford-base32 ULID, 26 characters). The {@code uid} is what appears in
 * REST URLs (behind a literal {@code /uid/} segment). The numeric {@code id}
 * must NEVER appear in a REST path — that would leak row order and invite
 * enumeration — but it MAY be carried in DTOs / response bodies, where it is
 * useful for client-side joins and table row keys.
 *
 * <p>ULIDs are lexicographically sortable and time-ordered, which keeps
 * indexed lookups and paginated listings efficient compared to random UUIDs.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    public static final int UID_LENGTH = 26;

    @Column(name = "uid", nullable = false, updatable = false, unique = true, length = UID_LENGTH)
    private String uid;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    @Version
    private Long version;

    @PrePersist
    void assignUid() {
        if (uid == null) {
            uid = UlidCreator.getMonotonicUlid().toString();
        }
    }
}
