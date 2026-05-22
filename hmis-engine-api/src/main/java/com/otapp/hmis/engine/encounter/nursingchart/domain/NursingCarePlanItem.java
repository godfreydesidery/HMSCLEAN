package com.otapp.hmis.engine.encounter.nursingchart.domain;

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
 * One row on the patient's nursing care plan (PROCESS.md §4): a nursing
 * problem / diagnosis with the matching goal, intervention, and
 * outcome evaluation. An admission accumulates many items as the stay
 * progresses; each carries its own ACTIVE → RESOLVED / CANCELLED status.
 */
@Entity
@Table(name = "nursing_care_plan_item",
       indexes = {
               @Index(name = "idx_nursing_care_admission", columnList = "admission_uid, opened_at"),
               @Index(name = "idx_nursing_care_status",    columnList = "status")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NursingCarePlanItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_uid", nullable = false, length = 26)
    private String admissionUid;

    @Setter @Column(nullable = false, length = 500)  private String problem;
    @Setter @Column(nullable = false, length = 500)  private String goal;
    @Setter @Column(nullable = false, length = 2000) private String intervention;
    @Setter @Column(length = 2000)                   private String evaluation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NursingCarePlanStatus status = NursingCarePlanStatus.ACTIVE;

    @Column(name = "opened_by_username", nullable = false, length = 64) private String openedByUsername;
    @Column(name = "opened_at",          nullable = false)              private Instant openedAt;

    @Setter @Column(name = "closed_by_username", length = 64)    private String closedByUsername;
    @Setter @Column(name = "closed_at")                          private Instant closedAt;
    @Setter @Column(name = "close_reason", length = 255)         private String closeReason;

    public NursingCarePlanItem(String admissionUid, String openedByUsername,
                               String problem, String goal, String intervention, String evaluation) {
        if (problem == null || problem.isBlank()) {
            throw new BusinessRuleException("problem is required");
        }
        if (goal == null || goal.isBlank()) {
            throw new BusinessRuleException("goal is required");
        }
        if (intervention == null || intervention.isBlank()) {
            throw new BusinessRuleException("intervention is required");
        }
        this.admissionUid = admissionUid;
        this.openedByUsername = openedByUsername;
        this.problem = problem;
        this.goal = goal;
        this.intervention = intervention;
        this.evaluation = evaluation;
        this.openedAt = Instant.now();
    }

    public void resolve(String username, String evaluation) {
        if (status != NursingCarePlanStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE items can be resolved (current: " + status + ")");
        }
        if (evaluation != null && !evaluation.isBlank()) {
            this.evaluation = evaluation;
        }
        this.status = NursingCarePlanStatus.RESOLVED;
        this.closedByUsername = username;
        this.closedAt = Instant.now();
    }

    public void cancel(String username, String reason) {
        if (status == NursingCarePlanStatus.CANCELLED) return;
        if (status == NursingCarePlanStatus.RESOLVED) {
            throw new BusinessRuleException("Resolved items cannot be cancelled");
        }
        this.status = NursingCarePlanStatus.CANCELLED;
        this.closedByUsername = username;
        this.closedAt = Instant.now();
        this.closeReason = reason;
    }

    public boolean isOpen() {
        return status == NursingCarePlanStatus.ACTIVE;
    }
}
