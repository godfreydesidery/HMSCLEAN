package com.otapp.hmis.engine.encounter.attachment.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
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

/**
 * A file attached to a clinical order — lab result PDF, radiology image,
 * intra-op photo, etc. The bytes live on disk under the configured
 * {@code hmis.attachments.dir}; this row carries the metadata + the
 * relative {@code storageKey} that points at them.
 */
@Entity
@Table(name = "order_attachment",
       indexes = {
               @Index(name = "idx_order_attachment_order", columnList = "order_uid"),
               @Index(name = "idx_order_attachment_uploaded", columnList = "uploaded_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAttachment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_uid", nullable = false, length = 26)
    private String orderUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_kind", nullable = false, length = 16)
    private ClinicalOrderKind orderKind;

    /** Original filename, as uploaded. */
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    /** MIME type as reported by the client (best-effort). */
    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /**
     * Relative path under the configured attachments directory.
     * Format: {orderUid}/{attachmentUid}-{filename}.
     */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "uploaded_by_username", nullable = false, length = 64)
    private String uploadedByUsername;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @SuppressWarnings("java:S107") // private-ish builder; 8 fields is irreducible without a builder
    public OrderAttachment(String orderUid, ClinicalOrderKind orderKind,
                           String filename, String contentType, long sizeBytes,
                           String storageKey, String description, String uploadedByUsername) {
        this.orderUid = orderUid;
        this.orderKind = orderKind;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.description = description;
        this.uploadedByUsername = uploadedByUsername;
        this.uploadedAt = Instant.now();
    }

    /**
     * Set the final storage key after the row is persisted and the uid is
     * known — the key format embeds the uid for collision-free per-order
     * subdirectories.
     */
    public void assignStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }
}
