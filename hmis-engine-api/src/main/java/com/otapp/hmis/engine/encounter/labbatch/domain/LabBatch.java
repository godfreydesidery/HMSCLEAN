package com.otapp.hmis.engine.encounter.labbatch.domain;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Groups N {@code ClinicalOrder} rows of kind LAB_TEST that share the
 * same {@code labTestTypeUid} so the lab tech can process them as a
 * single bench run. Purely organisational — does not modify the
 * individual order statuses. PROCESS.md §5.5.
 */
@Entity
@Table(name = "lab_batch",
       uniqueConstraints = @UniqueConstraint(name = "uk_lab_batch_no", columnNames = "batch_no"),
       indexes = {
               @Index(name = "idx_lab_batch_status", columnList = "status"),
               @Index(name = "idx_lab_batch_test",   columnList = "lab_test_type_uid")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabBatch extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no", nullable = false, length = 32)
    private String batchNo;

    @Column(name = "lab_test_type_uid", nullable = false, length = 26)
    private String labTestTypeUid;

    @Setter @Column(length = 500) private String note;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LabBatchStatus status = LabBatchStatus.OPEN;

    @Column(name = "opened_by_username", nullable = false, length = 64)
    private String openedByUsername;

    @Column(name = "opened_at", nullable = false) private Instant openedAt;
    @Setter @Column(name = "processed_at")        private Instant processedAt;
    @Setter @Column(name = "completed_at")        private Instant completedAt;
    @Setter @Column(name = "cancelled_at")        private Instant cancelledAt;
    @Setter @Column(name = "cancel_reason", length = 255) private String cancelReason;

    public LabBatch(String batchNo, String labTestTypeUid, String openedByUsername, String note) {
        this.batchNo = batchNo;
        this.labTestTypeUid = labTestTypeUid;
        this.openedByUsername = openedByUsername;
        this.note = note;
        this.openedAt = Instant.now();
    }

    public void markProcessing() {
        if (status != LabBatchStatus.OPEN) {
            throw new BusinessRuleException("Only OPEN batches can move to PROCESSING (current: " + status + ")");
        }
        status = LabBatchStatus.PROCESSING;
        processedAt = Instant.now();
    }

    public void markCompleted() {
        if (status != LabBatchStatus.PROCESSING && status != LabBatchStatus.OPEN) {
            throw new BusinessRuleException("Only OPEN / PROCESSING batches can be completed (current: " + status + ")");
        }
        status = LabBatchStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status == LabBatchStatus.COMPLETED) {
            throw new BusinessRuleException("Completed batches cannot be cancelled");
        }
        if (status == LabBatchStatus.CANCELLED) return;
        status = LabBatchStatus.CANCELLED;
        cancelledAt = Instant.now();
        cancelReason = reason;
    }

    public boolean isOpenForMembership() {
        return status == LabBatchStatus.OPEN;
    }
}
