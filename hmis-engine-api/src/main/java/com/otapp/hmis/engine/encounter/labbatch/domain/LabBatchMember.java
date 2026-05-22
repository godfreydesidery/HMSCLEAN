package com.otapp.hmis.engine.encounter.labbatch.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Membership row linking a {@link LabBatch} to one {@code ClinicalOrder}.
 * An order can only sit in one open batch at a time (enforced by the
 * unique constraint on order_uid).
 */
@Entity
@Table(name = "lab_batch_member",
       uniqueConstraints = @UniqueConstraint(name = "uk_lab_batch_member_order", columnNames = "order_uid"),
       indexes = @Index(name = "idx_lab_batch_member_batch", columnList = "batch_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabBatchMember extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_uid", nullable = false, length = 26)
    private String batchUid;

    @Column(name = "order_uid", nullable = false, length = 26)
    private String orderUid;

    public LabBatchMember(String batchUid, String orderUid) {
        this.batchUid = batchUid;
        this.orderUid = orderUid;
    }
}
