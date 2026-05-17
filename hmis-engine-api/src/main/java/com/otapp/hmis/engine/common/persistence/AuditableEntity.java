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
 * <p>Every entity has an internal numeric {@code id} (used only inside the
 * service / persistence layer) and an externally-facing {@code uid}
 * (Crockford-base32 ULID, 26 characters) that is what gets exposed in URLs,
 * DTOs and to the frontend. The numeric {@code id} must never appear in REST
 * paths or response bodies.
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
