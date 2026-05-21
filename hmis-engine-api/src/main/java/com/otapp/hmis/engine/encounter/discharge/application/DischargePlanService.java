package com.otapp.hmis.engine.encounter.discharge.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.DischargeRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionService;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.CancelPlanRequest;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.CreatePlanRequest;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.DischargePlanDto;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.UpdatePlanRequest;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlan;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanKind;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workflow service for the structured discharge plan. The plan is a
 * sibling aggregate to {@link Admission}: doctor authors the structured
 * narrative + kind-specific fields, ward administrator approves, and on
 * approval the underlying admission is closed via the kind's mapped
 * closure call (DISCHARGE → discharge, DECEASED → markDeceased,
 * REFERRAL → transferOut).
 */
@Service
@RequiredArgsConstructor
public class DischargePlanService {

    private final DischargePlanRepository planRepository;
    private final AdmissionRepository admissionRepository;
    private final AdmissionService admissionService;

    @Transactional
    public DischargePlanDto create(String admissionUid, CreatePlanRequest request) {
        Admission admission = loadAdmission(admissionUid);
        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException(
                    "Discharge plan can only be authored while admission is ADMITTED (current: "
                            + admission.getStatus() + ")");
        }
        if (planRepository.findByAdmissionUid(admissionUid).isPresent()) {
            throw new ConflictException("Admission already has a discharge plan");
        }

        DischargePlan plan = new DischargePlan(admission.getUid(), request.kind(), currentUsername());
        applyEditableFields(plan,
                request.history(), request.investigation(), request.management(),
                request.operationNote(), request.icuNote(), request.recommendations(),
                request.referralFacility(), request.referralReason(),
                request.timeOfDeath(), request.causeOfDeath());
        planRepository.save(plan);
        return toDto(plan, admission);
    }

    @Transactional
    public DischargePlanDto update(String admissionUid, UpdatePlanRequest request) {
        DischargePlan plan = loadByAdmission(admissionUid);
        plan.requireEditable();
        applyEditableFields(plan,
                request.history(), request.investigation(), request.management(),
                request.operationNote(), request.icuNote(), request.recommendations(),
                request.referralFacility(), request.referralReason(),
                request.timeOfDeath(), request.causeOfDeath());
        return toDto(plan, loadAdmission(admissionUid));
    }

    /**
     * Approves a PENDING plan and immediately drives the admission to the
     * corresponding terminal state. Validates kind-specific required
     * fields are present before approving.
     */
    @Transactional
    public DischargePlanDto approve(String admissionUid) {
        DischargePlan plan = loadByAdmission(admissionUid);
        Admission admission = loadAdmission(admissionUid);
        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleException(
                    "Cannot approve plan: admission is " + admission.getStatus());
        }
        validateRequiredForApproval(plan);

        plan.approve(currentUsername());

        // Drive the admission closure through AdmissionService so the closure
        // gate (which now requires this APPROVED plan) and bed release run in
        // one place. The structured fields stay on the plan; the admission's
        // free-text dischargeSummary is set to a short pointer.
        DischargeRequest pointer = new DischargeRequest("See discharge plan " + plan.getUid());
        switch (plan.getKind()) {
            case DISCHARGE -> admissionService.discharge(admissionUid, pointer);
            case DECEASED -> admissionService.markDeceased(admissionUid, pointer);
            case REFERRAL -> admissionService.transferOut(admissionUid, pointer);
        }
        return toDto(plan, loadAdmission(admissionUid));
    }

    @Transactional
    public DischargePlanDto cancel(String admissionUid, CancelPlanRequest request) {
        DischargePlan plan = loadByAdmission(admissionUid);
        plan.cancel(currentUsername(), emptyToNull(request == null ? null : request.reason()));
        return toDto(plan, loadAdmission(admissionUid));
    }

    @Transactional(readOnly = true)
    public DischargePlanDto findByAdmission(String admissionUid) {
        DischargePlan plan = loadByAdmission(admissionUid);
        return toDto(plan, loadAdmission(admissionUid));
    }

    // ----- helpers ---------------------------------------------------------

    private Admission loadAdmission(String admissionUid) {
        return admissionRepository.findByUid(admissionUid)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + admissionUid));
    }

    private DischargePlan loadByAdmission(String admissionUid) {
        return planRepository.findByAdmissionUid(admissionUid)
                .orElseThrow(() -> new NotFoundException(
                        "No discharge plan for admission: " + admissionUid));
    }

    private static void applyEditableFields(DischargePlan plan,
                                            String history, String investigation, String management,
                                            String operationNote, String icuNote, String recommendations,
                                            String referralFacility, String referralReason,
                                            java.time.Instant timeOfDeath, String causeOfDeath) {
        plan.setHistory(emptyToNull(history));
        plan.setInvestigation(emptyToNull(investigation));
        plan.setManagement(emptyToNull(management));
        plan.setOperationNote(emptyToNull(operationNote));
        plan.setIcuNote(emptyToNull(icuNote));
        plan.setRecommendations(emptyToNull(recommendations));
        plan.setReferralFacility(emptyToNull(referralFacility));
        plan.setReferralReason(emptyToNull(referralReason));
        plan.setTimeOfDeath(timeOfDeath);
        plan.setCauseOfDeath(emptyToNull(causeOfDeath));
    }

    private static void validateRequiredForApproval(DischargePlan plan) {
        if (isBlank(plan.getHistory()) || isBlank(plan.getManagement())
                || isBlank(plan.getRecommendations())) {
            throw new BusinessRuleException(
                    "history, management and recommendations are required before approval");
        }
        if (plan.getKind() == DischargePlanKind.REFERRAL) {
            if (isBlank(plan.getReferralFacility()) || isBlank(plan.getReferralReason())) {
                throw new BusinessRuleException(
                        "REFERRAL plans require referralFacility and referralReason before approval");
            }
        }
        if (plan.getKind() == DischargePlanKind.DECEASED) {
            if (plan.getTimeOfDeath() == null || isBlank(plan.getCauseOfDeath())) {
                throw new BusinessRuleException(
                        "DECEASED plans require timeOfDeath and causeOfDeath before approval");
            }
        }
    }

    private DischargePlanDto toDto(DischargePlan p, Admission admission) {
        return new DischargePlanDto(
                p.getUid(),
                p.getAdmissionUid(),
                admission == null ? null : admission.getAdmissionNo(),
                p.getKind(),
                p.getStatus(),
                p.getHistory(), p.getInvestigation(), p.getManagement(),
                p.getOperationNote(), p.getIcuNote(), p.getRecommendations(),
                p.getReferralFacility(), p.getReferralReason(),
                p.getTimeOfDeath(), p.getCauseOfDeath(),
                p.getAuthoredByUsername(), p.getAuthoredAt(),
                p.getApprovedByUsername(), p.getApprovedAt(),
                p.getCancelledByUsername(), p.getCancelledAt(), p.getCancelReason(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user required");
        }
        return auth.getName();
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
