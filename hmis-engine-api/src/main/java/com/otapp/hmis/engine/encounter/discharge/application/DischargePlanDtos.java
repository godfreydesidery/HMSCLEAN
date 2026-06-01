package com.otapp.hmis.engine.encounter.discharge.application;

import com.otapp.hmis.engine.encounter.discharge.domain.ClosureSubject;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanKind;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class DischargePlanDtos {

    private DischargePlanDtos() {}

    // ----- requests --------------------------------------------------------

    /**
     * Creates a discharge plan for an admission. {@code kind} fixes the
     * closure path (DISCHARGE / DECEASED / REFERRAL) — it cannot be
     * changed afterwards. Clinical fields can be filled in here or via a
     * later {@link UpdatePlanRequest}.
     */
    public record CreatePlanRequest(
            @NotNull DischargePlanKind kind,
            @Size(max = 4000) String history,
            @Size(max = 4000) String investigation,
            @Size(max = 4000) String management,
            @Size(max = 4000) String operationNote,
            @Size(max = 4000) String icuNote,
            @Size(max = 4000) String recommendations,
            @Size(max = 200)  String referralFacility,
            @Size(max = 26)   String externalProviderUid,
            @Size(max = 1000) String referralReason,
            Instant timeOfDeath,
            @Size(max = 500)  String causeOfDeath) {}

    /** Update the editable narrative + kind-specific fields while still PENDING. */
    public record UpdatePlanRequest(
            @Size(max = 4000) String history,
            @Size(max = 4000) String investigation,
            @Size(max = 4000) String management,
            @Size(max = 4000) String operationNote,
            @Size(max = 4000) String icuNote,
            @Size(max = 4000) String recommendations,
            @Size(max = 200)  String referralFacility,
            @Size(max = 26)   String externalProviderUid,
            @Size(max = 1000) String referralReason,
            Instant timeOfDeath,
            @Size(max = 500)  String causeOfDeath) {}

    public record CancelPlanRequest(@Size(max = 255) String reason) {}

    // ----- responses -------------------------------------------------------

    public record DischargePlanDto(
            String uid,
            ClosureSubject subjectType,
            String admissionUid,
            String admissionNo,
            String consultationUid,
            String consultationNo,
            DischargePlanKind kind,
            DischargePlanStatus status,
            String history,
            String investigation,
            String management,
            String operationNote,
            String icuNote,
            String recommendations,
            String referralFacility,
            String externalProviderUid,
            String externalProviderName,
            String referralReason,
            Instant timeOfDeath,
            String causeOfDeath,
            String authoredByUsername,
            Instant authoredAt,
            String approvedByUsername,
            Instant approvedAt,
            String cancelledByUsername,
            Instant cancelledAt,
            String cancelReason,
            Instant createdAt,
            Instant updatedAt) {}
}
