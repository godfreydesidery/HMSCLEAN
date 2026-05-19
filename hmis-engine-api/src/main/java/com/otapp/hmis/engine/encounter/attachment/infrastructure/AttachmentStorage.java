package com.otapp.hmis.engine.encounter.attachment.infrastructure;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Filesystem-backed blob store for attachment bytes. The root directory is
 * configurable via {@code hmis.attachments.dir} — defaults to
 * {@code ${java.io.tmpdir}/hmis-attachments} for dev / tests; production
 * deployments should set it to a persistent volume mount.
 *
 * <p>Storage keys are relative paths like {@code orderUid/attachmentUid-filename};
 * the service composes them. This component just maps key ↔ absolute path
 * + read / write / delete.
 */
@Component
@Slf4j
public class AttachmentStorage {

    private final Path root;

    public AttachmentStorage(@Value("${hmis.attachments.dir:${java.io.tmpdir}/hmis-attachments}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureRootExists() throws IOException {
        Files.createDirectories(root);
        log.info("Attachment storage root: {}", root);
    }

    /** Stream {@code data} into {@code storageKey}; creates parent dirs as needed. */
    public void put(String storageKey, InputStream data) {
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to store attachment: " + e.getMessage());
        }
    }

    /** Open the stored bytes for streaming back to the client. */
    public Resource read(String storageKey) {
        Path source = resolve(storageKey);
        if (!Files.exists(source)) {
            throw new BusinessRuleException("Attachment bytes missing on disk: " + storageKey);
        }
        return new FileSystemResource(source);
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            log.warn("Failed to delete attachment bytes for {}: {}", storageKey, e.getMessage());
        }
    }

    private Path resolve(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessRuleException("Storage key escapes the attachments root: " + storageKey);
        }
        return resolved;
    }
}
