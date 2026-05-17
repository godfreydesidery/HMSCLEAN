package com.otapp.hmis.engine.encounter.result.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_result",
       uniqueConstraints = @UniqueConstraint(name = "uk_order_result_order", columnNames = "order_uid"),
       indexes = {
               @Index(name = "idx_order_result_status", columnList = "status")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderResult extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_uid", nullable = false, length = 26)
    private String orderUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_kind", nullable = false, length = 16)
    private ClinicalOrderKind orderKind;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderResultStatus status = OrderResultStatus.PRELIMINARY;

    /** Findings, observations, narrative report body. */
    @Setter
    @Column(length = 8000)
    private String narrative;

    /** Short conclusion / impression / abnormal flag — surfaced on the order. */
    @Setter
    @Column(length = 1000)
    private String impression;

    @Setter @Column(name = "finalized_at")  private Instant finalizedAt;
    @Setter @Column(name = "finalized_by", length = 80) private String finalizedBy;
    @Setter @Column(name = "amended_at")    private Instant amendedAt;
    @Setter @Column(name = "amended_by", length = 80)   private String amendedBy;

    public OrderResult(String orderUid, ClinicalOrderKind orderKind, String narrative, String impression) {
        this.orderUid = orderUid;
        this.orderKind = orderKind;
        this.narrative = narrative;
        this.impression = impression;
    }

    public void editPreliminary(String narrative, String impression) {
        if (status != OrderResultStatus.PRELIMINARY) {
            throw new BusinessRuleException("Result has been finalized; use amend to change it.");
        }
        this.narrative = narrative;
        this.impression = impression;
    }

    public void finalize(String username) {
        if (status != OrderResultStatus.PRELIMINARY) {
            throw new BusinessRuleException("Only PRELIMINARY results can be finalized (current: " + status + ")");
        }
        this.status = OrderResultStatus.FINAL;
        this.finalizedAt = Instant.now();
        this.finalizedBy = username;
    }

    public void amend(String narrative, String impression, String username) {
        if (status == OrderResultStatus.PRELIMINARY) {
            throw new BusinessRuleException("Preliminary results should be edited, not amended.");
        }
        this.narrative = narrative;
        this.impression = impression;
        this.status = OrderResultStatus.AMENDED;
        this.amendedAt = Instant.now();
        this.amendedBy = username;
    }
}
