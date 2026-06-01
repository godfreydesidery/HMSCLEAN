package com.otapp.hmis.engine.encounter.discharge.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.DischargeRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionService;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationService;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.CancelPlanRequest;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.CreatePlanRequest;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.DischargePlanDto;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.UpdatePlanRequest;
import com.otapp.hmis.engine.encounter.discharge.domain.ClosureSubject;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlan;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanKind;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanRepository;
import com.otapp.hmis.engine.masterdata.externalprovider.domain.ExternalMedicalProvider;
import com.otapp.hmis.engine.masterdata.externalprovider.domain.ExternalMedicalProviderRepository;
import com.otapp.hmis.engine.patient.application.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workflow service for the unified closure plan. A plan is a sibling aggregate
 * to {@link Admission} (inpatient) or {@link Consultation} (outpatient): the
 * clinician authors the structured narrative + kind-specific fields, a second
 * user approves, and on approval the underlying encounter is closed.
 *
 * <ul>
 *   <li>ADMISSION subject → DISCHARGE / DECEASED / REFERRAL drive
 *       {@code AdmissionService.discharge / markDeceased / transferOut}.</li>
 *   <li>CONSULTATION subject → DECEASED / REFERRAL drive
 *       {@code ConsultationService.closeAsDeceased / closeAsReferred}.</li>
 * </ul>
 *
 * A DECEASED approval (either subject) also flags the patient deceased so they
 * can no longer be re-booked / re-admitted.
 */
@Service
@RequiredArgsConstructor
public class DischargePlanService {

    private final DischargePlanRepository planRepository;
    private final AdmissionRepository admissionRepository;
    private final AdmissionService admissionService;
    private final ConsultationRepository consultationRepository;
    private final ConsultationService consultationService;
    private final PatientService patientService;
    private final ExternalMedicalProviderRepository externalProviderRepository;

    // ===== Admission-subject closure ======================================

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

        DischargePlan plan = DischargePlan.forAdmission(admission.getUid(), request.kind(), currentUsername());
        applyEditableFields(plan, request);
        planRepository.save(plan);
        return toDto(plan);
    }

    @Transactional
    public DischargePlanDto update(String admissionUid, UpdatePlanRequest request) {
        DischargePlan plan = loadByAdmission(admissionUid);
        plan.requireEditable();
        applyEditableFields(plan, request);
        return toDto(plan);
    }

    /**
     * Approves a PENDING admission plan and immediately drives the admission to
     * the corresponding terminal state.
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
            case DECEASED -> {
                admissionService.markDeceased(admissionUid, pointer);
                patientService.markDeceased(admission.getPatientUid(), plan.getTimeOfDeath());
            }
            case REFERRAL -> admissionService.transferOut(admissionUid, pointer);
        }
        return toDto(plan);
    }

    @Transactional
    public DischargePlanDto cancel(String admissionUid, CancelPlanRequest request) {
        DischargePlan plan = loadByAdmission(admissionUid);
        plan.cancel(currentUsername(), emptyToNull(request == null ? null : request.reason()));
        return toDto(plan);
    }

    @Transactional(readOnly = true)
    public DischargePlanDto findByAdmission(String admissionUid) {
        return toDto(loadByAdmission(admissionUid));
    }

    // ===== Consultation-subject closure ===================================

    @Transactional
    public DischargePlanDto createForConsultation(String consultationUid, CreatePlanRequest request) {
        Consultation consultation = loadConsultation(consultationUid);
        requireOpenConsultation(consultation, "authored");
        if (planRepository.findByConsultationUid(consultationUid).isPresent()) {
            throw new ConflictException("Consultation already has a closure plan");
        }

        DischargePlan plan = DischargePlan.forConsultation(consultation.getUid(), request.kind(), currentUsername());
        applyEditableFields(plan, request);
        planRepository.save(plan);
        return toDto(plan);
    }

    @Transactional
    public DischargePlanDto updateForConsultation(String consultationUid, UpdatePlanRequest request) {
        DischargePlan plan = loadByConsultation(consultationUid);
        plan.requireEditable();
        applyEditableFields(plan, request);
        return toDto(plan);
    }

    /**
     * Approves a PENDING consultation closure plan and immediately closes the
     * consultation (DECEASED / REFERRED). A DECEASED approval also flags the
     * patient deceased.
     */
    @Transactional
    public DischargePlanDto approveForConsultation(String consultationUid) {
        DischargePlan plan = loadByConsultation(consultationUid);
        Consultation consultation = loadConsultation(consultationUid);
        requireOpenConsultation(consultation, "approved");
        validateRequiredForApproval(plan);

        plan.approve(currentUsername());

        switch (plan.getKind()) {
            case DECEASED -> {
                consultationService.closeAsDeceased(consultationUid);
                patientService.markDeceased(consultation.getPatientUid(), plan.getTimeOfDeath());
            }
            case REFERRAL -> consultationService.closeAsReferred(consultationUid);
            case DISCHARGE -> throw new BusinessRuleException(
                    "DISCHARGE is not a valid consultation closure kind");
        }
        return toDto(plan);
    }

    @Transactional
    public DischargePlanDto cancelForConsultation(String consultationUid, CancelPlanRequest request) {
        DischargePlan plan = loadByConsultation(consultationUid);
        plan.cancel(currentUsername(), emptyToNull(request == null ? null : request.reason()));
        return toDto(plan);
    }

    @Transactional(readOnly = true)
    public DischargePlanDto findByConsultation(String consultationUid) {
        return toDto(loadByConsultation(consultationUid));
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

    private Consultation loadConsultation(String consultationUid) {
        return consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));
    }

    private DischargePlan loadByConsultation(String consultationUid) {
        return planRepository.findByConsultationUid(consultationUid)
                .orElseThrow(() -> new NotFoundException(
                        "No closure plan for consultation: " + consultationUid));
    }

    private static void requireOpenConsultation(Consultation consultation, String verb) {
        if (consultation.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "A closure plan can only be " + verb + " while the consultation is IN_PROGRESS (current: "
                            + consultation.getStatus() + ")");
        }
    }

    private void applyEditableFields(DischargePlan plan, CreatePlanRequest r) {
        applyNarrative(plan, r.history(), r.investigation(), r.management(),
                r.operationNote(), r.icuNote(), r.recommendations());
        applyReferral(plan, r.referralFacility(), r.externalProviderUid(), r.referralReason());
        plan.setTimeOfDeath(r.timeOfDeath());
        plan.setCauseOfDeath(emptyToNull(r.causeOfDeath()));
    }

    private void applyEditableFields(DischargePlan plan, UpdatePlanRequest r) {
        applyNarrative(plan, r.history(), r.investigation(), r.management(),
                r.operationNote(), r.icuNote(), r.recommendations());
        applyReferral(plan, r.referralFacility(), r.externalProviderUid(), r.referralReason());
        plan.setTimeOfDeath(r.timeOfDeath());
        plan.setCauseOfDeath(emptyToNull(r.causeOfDeath()));
    }

    private static void applyNarrative(DischargePlan plan,
                                       String history, String investigation, String management,
                                       String operationNote, String icuNote, String recommendations) {
        plan.setHistory(emptyToNull(history));
        plan.setInvestigation(emptyToNull(investigation));
        plan.setManagement(emptyToNull(management));
        plan.setOperationNote(emptyToNull(operationNote));
        plan.setIcuNote(emptyToNull(icuNote));
        plan.setRecommendations(emptyToNull(recommendations));
    }

    /**
     * Resolve the referral target. If an external provider uid is given it wins:
     * the provider is validated and its name denormalised onto referralFacility.
     * Otherwise the free-text facility (if any) is kept.
     */
    private void applyReferral(DischargePlan plan, String referralFacility,
                               String externalProviderUid, String referralReason) {
        String providerUid = emptyToNull(externalProviderUid);
        if (providerUid != null) {
            ExternalMedicalProvider provider = externalProviderRepository.findByUid(providerUid)
                    .orElseThrow(() -> new NotFoundException("External provider not found: " + providerUid));
            plan.setExternalProviderUid(provider.getUid());
            plan.setReferralFacility(provider.getName());
        } else {
            plan.setExternalProviderUid(null);
            plan.setReferralFacility(emptyToNull(referralFacility));
        }
        plan.setReferralReason(emptyToNull(referralReason));
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
                        "REFERRAL plans require a referral facility (or external provider) and referral reason before approval");
            }
        }
        if (plan.getKind() == DischargePlanKind.DECEASED) {
            if (plan.getTimeOfDeath() == null || isBlank(plan.getCauseOfDeath())) {
                throw new BusinessRuleException(
                        "DECEASED plans require timeOfDeath and causeOfDeath before approval");
            }
        }
    }

    private DischargePlanDto toDto(DischargePlan p) {
        String admissionNo = null;
        String consultationNo = null;
        if (p.getSubjectType() == ClosureSubject.ADMISSION && p.getAdmissionUid() != null) {
            admissionNo = admissionRepository.findByUid(p.getAdmissionUid())
                    .map(Admission::getAdmissionNo).orElse(null);
        } else if (p.getSubjectType() == ClosureSubject.CONSULTATION && p.getConsultationUid() != null) {
            consultationNo = consultationRepository.findByUid(p.getConsultationUid())
                    .map(Consultation::getConsultationNo).orElse(null);
        }
        String externalProviderName = p.getExternalProviderUid() == null
                ? null
                : externalProviderRepository.findByUid(p.getExternalProviderUid())
                        .map(ExternalMedicalProvider::getName).orElse(p.getReferralFacility());

        return new DischargePlanDto(
                p.getUid(),
                p.getSubjectType(),
                p.getAdmissionUid(),
                admissionNo,
                p.getConsultationUid(),
                consultationNo,
                p.getKind(),
                p.getStatus(),
                p.getHistory(), p.getInvestigation(), p.getManagement(),
                p.getOperationNote(), p.getIcuNote(), p.getRecommendations(),
                p.getReferralFacility(), p.getExternalProviderUid(), externalProviderName,
                p.getReferralReason(),
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
