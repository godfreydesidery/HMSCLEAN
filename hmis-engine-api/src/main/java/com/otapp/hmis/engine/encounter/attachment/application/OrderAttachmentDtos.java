package com.otapp.hmis.engine.encounter.attachment.application;

import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import java.time.Instant;

public final class OrderAttachmentDtos {

    private OrderAttachmentDtos() {}

    public record OrderAttachmentDto(
            String uid,
            String orderUid,
            ClinicalOrderKind orderKind,
            String filename,
            String contentType,
            long sizeBytes,
            String description,
            String uploadedByUsername,
            Instant uploadedAt,
            Instant createdAt) {}
}
