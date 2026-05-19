package com.otapp.hmis.engine.encounter.attachment.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.attachment.application.OrderAttachmentDtos.OrderAttachmentDto;
import com.otapp.hmis.engine.encounter.attachment.domain.OrderAttachment;
import com.otapp.hmis.engine.encounter.attachment.domain.OrderAttachmentRepository;
import com.otapp.hmis.engine.encounter.attachment.infrastructure.AttachmentStorage;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrder;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OrderAttachmentService {

    /** Hard cap to keep someone uploading a 5 GiB file from filling the disk. */
    private static final long MAX_SIZE_BYTES = 25L * 1024 * 1024; // 25 MiB

    private final OrderAttachmentRepository attachmentRepository;
    private final ClinicalOrderRepository orderRepository;
    private final AttachmentStorage storage;

    @Transactional
    public OrderAttachmentDto upload(String orderUid, MultipartFile file, String description) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Attachment file is required and must be non-empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessRuleException(
                    "Attachment is " + file.getSize() + " bytes; max is " + MAX_SIZE_BYTES);
        }

        ClinicalOrder order = orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderUid));

        String originalFilename = sanitize(file.getOriginalFilename(), "attachment");
        String contentType = (file.getContentType() == null || file.getContentType().isBlank())
                ? "application/octet-stream"
                : file.getContentType();

        OrderAttachment attachment = new OrderAttachment(
                order.getUid(), order.getKind(),
                originalFilename, contentType, file.getSize(),
                "",                            // storage key set after save so we have the uid
                emptyToNull(description),
                currentUsername());
        attachmentRepository.save(attachment);

        String storageKey = order.getUid() + "/" + attachment.getUid() + "-" + originalFilename;
        try (InputStream in = file.getInputStream()) {
            storage.put(storageKey, in);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to read upload stream: " + e.getMessage());
        }

        // Persist the final storage key. JPA dirty-tracking handles the update.
        attachment.assignStorageKey(storageKey);
        return toDto(attachment);
    }

    @Transactional(readOnly = true)
    public List<OrderAttachmentDto> list(String orderUid) {
        // Verify the order exists so callers get a clean 404 rather than an empty list.
        orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderUid));
        return attachmentRepository.findAllByOrderUidOrderByUploadedAtAsc(orderUid).stream()
                .map(OrderAttachmentService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadHandle download(String attachmentUid) {
        OrderAttachment attachment = loadOrThrow(attachmentUid);
        Resource resource = storage.read(attachment.getStorageKey());
        return new DownloadHandle(attachment.getFilename(), attachment.getContentType(),
                attachment.getSizeBytes(), resource);
    }

    @Transactional
    public void delete(String attachmentUid) {
        OrderAttachment attachment = loadOrThrow(attachmentUid);
        storage.delete(attachment.getStorageKey());
        attachmentRepository.delete(attachment);
    }

    private OrderAttachment loadOrThrow(String uid) {
        return attachmentRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Attachment not found: " + uid));
    }

    /**
     * Strips path separators and limits length so a malicious upload can't
     * escape the per-order subdirectory. The aggregate's uid prefix on the
     * storage key keeps collisions impossible even after sanitisation.
     */
    private static String sanitize(String filename, String fallback) {
        if (filename == null || filename.isBlank()) return fallback;
        String base = filename.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (base.length() > 120) base = base.substring(0, 120);
        return base.isEmpty() ? fallback : base;
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user required");
        }
        return auth.getName();
    }

    private static OrderAttachmentDto toDto(OrderAttachment a) {
        return new OrderAttachmentDto(
                a.getUid(),
                a.getOrderUid(),
                a.getOrderKind(),
                a.getFilename(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getDescription(),
                a.getUploadedByUsername(),
                a.getUploadedAt(),
                a.getCreatedAt());
    }

    public record DownloadHandle(String filename, String contentType, long sizeBytes, Resource resource) {}
}
