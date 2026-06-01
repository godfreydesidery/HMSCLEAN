package com.otapp.hmis.engine.encounter.admission.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.AdmissionDto;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.AdmissionSummary;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.AdmitPatientRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.CancelAdmissionRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.DischargeRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.TransferWardRequest;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.admission.application.event.AdmissionAdmittedEvent;
import com.otapp.hmis.engine.encounter.admission.application.event.AdmissionCancelledEvent;
import com.otapp.hmis.engine.encounter.admission.infrastructure.AdmissionNumberGenerator;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlan;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanKind;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanRepository;
import com.otapp.hmis.engine.encounter.discharge.domain.DischargePlanStatus;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.masterdata.bed.domain.Bed;
import com.otapp.hmis.engine.masterdata.bed.domain.BedRepository;
import com.otapp.hmis.engine.masterdata.bed.domain.BedStatus;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.ward.domain.Ward;
import com.otapp.hmis.engine.masterdata.ward.domain.WardRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final PatientRepository patientRepository;
    private final com.otapp.hmis.engine.patient.application.PatientService patientService;
    private final WardRepository wardRepository;
    private final BedRepository bedRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;
    private final DischargePlanRepository dischargePlanRepository;
    private final AdmissionNumberGenerator numberGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AdmissionDto admit(AdmitPatientRequest request) {
        Patient patient = patientRepository.findByUid(request.patientUid())
                .orElseThrow(() -> new NotFoundException("Patient not found: " + request.patientUid()));
        if (patient.isDeceased()) {
            throw new BusinessRuleException("Patient is recorded as deceased and cannot be admitted");
        }
        if (!patient.isActive()) {
            throw new BusinessRuleException("Cannot admit an inactive patient");
        }
        if (admissionRepository.existsByPatientUidAndStatusIn(patient.getUid(), AdmissionStatus.ACTIVE)) {
            throw new BusinessRuleException("Patient already has an active admission");
        }

        Ward ward = wardRepository.findByUid(request.wardUid())
                .orElseThrow(() -> new NotFoundException("Ward not found: " + request.wardUid()));
        if (!ward.isActive()) {
            throw new BusinessRuleException("Ward is not active: " + ward.getName());
        }

        User clinician = userRepository.findByUsername(request.admittingClinicianUsername())
                .orElseThrow(() -> new NotFoundException("Clinician not found: " + request.admittingClinicianUsername()));
        if (!clinician.isEnabled()) {
            throw new BusinessRuleException("Clinician account is disabled");
        }

        boolean needsPlan = request.paymentType() == PaymentType.INSURANCE
                || request.paymentType() == PaymentType.MIXED;
        String planUid = emptyToNull(request.insurancePlanUid());
        if (needsPlan && planUid == null) {
            throw new BusinessRuleException("Insurance plan is required for payment type " + request.paymentType());
        }
        if (planUid != null) {
            insurancePlanRepository.findByUid(planUid)
                    .orElseThrow(() -> new NotFoundException("Insurance plan not found: " + planUid));
        }

        String consultationUid = emptyToNull(request.consultationUid());
        if (consultationUid != null) {
            consultationRepository.findByUid(consultationUid)
                    .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));
        }

        Admission admission = new Admission(
                numberGenerator.next(),
                patient.getUid(),
                ward.getUid(),
                emptyToNull(request.bedLabel()),
                clinician.getUsername(),
                request.paymentType(),
                planUid,
                consultationUid,
                emptyToNull(request.admissionReason()));
        // Legacy deposit gate (Zana-HMIS doAdmission): a patient who owes out-of-pocket
        // for the bed is admitted deposit-pending — the ward-bed bill is still issued
        // (after-commit) but the bed is only RESERVED until the bill is settled, at which
        // point billing flips the admission to ADMITTED and the bed to OCCUPIED. CASH and
        // MIXED (insurance + cash top-up) both owe cash, so both are gated, mirroring
        // legacy's cash + partial-insurance-top-up branches; pure INSURANCE occupies
        // immediately (legacy's fully-covered branch).
        // KNOWN simplification: admission ward lines are not yet coverage-routed, so an
        // insured-but-uncovered ward (pure INSURANCE, plan doesn't cover the ward) still
        // occupies immediately here where legacy would hold it pending — to be addressed
        // when admission coverage routing lands.
        boolean depositRequired = request.paymentType() == PaymentType.CASH
                || request.paymentType() == PaymentType.MIXED;
        if (depositRequired) {
            admission.markAwaitingDeposit();
        }
        admissionRepository.save(admission);

        // Optional bed assignment — reserve (RESERVED) for a deposit-pending admission,
        // claim (OCCUPIED) otherwise; overwrite the free-text label with Bed.label.
        String bedUid = emptyToNull(request.bedUid());
        if (bedUid != null) {
            Bed bed = loadBedInWard(bedUid, ward.getUid());
            if (depositRequired) {
                bed.reserve(admission.getUid());
            } else {
                bed.claim(admission.getUid());
            }
            admission.setBedUid(bed.getUid());
            admission.setBedLabel(bed.getLabel());
        }
        patientService.touchLastVisit(patient.getUid());
        // Billing seeds + issues the admission (ward-bed) invoice on this event,
        // after-commit — so the bill exists and the discharge gate is armed the
        // moment a patient is admitted (legacy parity). Encounter never imports
        // billing; the dependency runs billing -> encounter via the event.
        eventPublisher.publishEvent(new AdmissionAdmittedEvent(admission.getUid()));
        return toDto(admission);
    }

    @Transactional
    public AdmissionDto transferWard(String uid, TransferWardRequest request) {
        Admission admission = loadOrThrow(uid);
        Ward ward = wardRepository.findByUid(request.wardUid())
                .orElseThrow(() -> new NotFoundException("Ward not found: " + request.wardUid()));
        if (!ward.isActive()) {
            throw new BusinessRuleException("Ward is not active: " + ward.getName());
        }
        // Release the prior bed (if any) before flipping wards. The
        // transferWard guard on the entity still enforces status=ADMITTED.
        releaseCurrentBed(admission);
        admission.transferWard(ward.getUid(), emptyToNull(request.bedLabel()));
        admission.setBedUid(null);

        String bedUid = emptyToNull(request.bedUid());
        if (bedUid != null) {
            Bed bed = loadBedInWard(bedUid, ward.getUid());
            bed.claim(admission.getUid());
            admission.setBedUid(bed.getUid());
            admission.setBedLabel(bed.getLabel());
        }
        return toDto(admission);
    }

    @Transactional
    public AdmissionDto discharge(String uid, DischargeRequest request) {
        Admission admission = loadOrThrow(uid);
        requireApprovedPlan(uid, DischargePlanKind.DISCHARGE);
        admission.discharge(emptyToNull(request == null ? null : request.summary()));
        releaseCurrentBed(admission);
        return toDto(admission);
    }

    @Transactional
    public AdmissionDto markDeceased(String uid, DischargeRequest request) {
        Admission admission = loadOrThrow(uid);
        requireApprovedPlan(uid, DischargePlanKind.DECEASED);
        admission.markDeceased(emptyToNull(request == null ? null : request.summary()));
        releaseCurrentBed(admission);
        return toDto(admission);
    }

    @Transactional
    public AdmissionDto transferOut(String uid, DischargeRequest request) {
        Admission admission = loadOrThrow(uid);
        requireApprovedPlan(uid, DischargePlanKind.REFERRAL);
        admission.transferOut(emptyToNull(request == null ? null : request.summary()));
        releaseCurrentBed(admission);
        return toDto(admission);
    }

    /**
     * Legacy gate (PROCESS_MISMATCHES.md M17): an admission may only be closed
     * once a discharge plan of the matching kind has been APPROVED by the ward
     * administrator. The discharge-plan approval path drives closure through
     * here, so the plan is APPROVED by the time this runs.
     */
    private void requireApprovedPlan(String admissionUid, DischargePlanKind kind) {
        DischargePlan plan = dischargePlanRepository.findByAdmissionUid(admissionUid).orElse(null);
        if (plan == null || plan.getStatus() != DischargePlanStatus.APPROVED || plan.getKind() != kind) {
            throw new BusinessRuleException(
                    "An APPROVED " + kind + " discharge plan is required before closing the admission");
        }
    }

    /**
     * Idempotent — flips the admission bill-clearance gate to cleared. Called by the
     * billing {@code SettlementDispatcher} once the admission invoice is fully settled
     * (PAID). Billing pushes this in the allowed billing -> encounter direction so the
     * closure gate can read a purely local flag (no encounter -> billing import).
     */
    @Transactional
    public void markBillsCleared(String admissionUid) {
        admissionRepository.findByUid(admissionUid).ifPresent(Admission::markBillsCleared);
    }

    /**
     * Idempotent — re-arms the admission bill-clearance gate. Called by billing when an
     * outstanding admission invoice is issued (or a refund / partial credit re-opens a
     * positive balance), so a subsequent discharge / referral / deceased closure is blocked
     * until the bill is settled again.
     */
    @Transactional
    public void clearBillsCleared(String admissionUid) {
        admissionRepository.findByUid(admissionUid).ifPresent(Admission::clearBillsClearedFlag);
    }

    /**
     * Activate a deposit-pending admission once its ward-bed bill is settled (legacy
     * {@code confirmBillsPayment}: PENDING → IN-PROCESS, WAITING → OCCUPIED). Billing
     * pushes this from the {@code SettlementDispatcher} when the admission invoice is
     * PAID — or has nothing left to owe. Flips AWAITING_DEPOSIT → ADMITTED and occupies
     * the reserved bed. Idempotent: a no-op once already ADMITTED or in a terminal
     * state, so a re-fired settlement (or a cancelled admission) never re-occupies a bed.
     */
    @Transactional
    public void confirmDeposit(String admissionUid) {
        admissionRepository.findByUid(admissionUid).ifPresent(admission -> {
            if (admission.confirmDeposit() && admission.getBedUid() != null) {
                bedRepository.findByUid(admission.getBedUid()).ifPresent(bed -> {
                    // Occupy the held bed — but NEVER throw: confirmDeposit runs inside the
                    // cashier's payment transaction, so a surprise bed state (e.g. an admin
                    // took it out of service) must not roll back the payment. The admission
                    // still activates; the bed is just not re-occupied. Bed-state mutators
                    // (out-of-service / free / delete) already block RESERVED, so in
                    // practice the bed is RESERVED here.
                    if (bed.getStatus() == BedStatus.RESERVED || bed.getStatus() == BedStatus.OCCUPIED) {
                        bed.occupy();
                    } else {
                        log.warn("Deposit settled for admission {} but its bed {} is {} — "
                                + "activating the admission without occupying the bed",
                                admissionUid, admission.getBedUid(), bed.getStatus());
                    }
                });
            }
        });
    }

    @Transactional
    public AdmissionDto cancel(String uid, CancelAdmissionRequest request) {
        Admission admission = loadOrThrow(uid);
        admission.cancel(emptyToNull(request == null ? null : request.reason()));
        releaseCurrentBed(admission);
        // Billing voids the (now-abandoned) ward-bed invoice after-commit, so a
        // cancelled deposit-pending admission leaves no live unpaid receivable.
        eventPublisher.publishEvent(new AdmissionCancelledEvent(admission.getUid()));
        return toDto(admission);
    }

    private Bed loadBedInWard(String bedUid, String wardUid) {
        Bed bed = bedRepository.findByUid(bedUid)
                .orElseThrow(() -> new NotFoundException("Bed not found: " + bedUid));
        if (!bed.getWardUid().equals(wardUid)) {
            throw new BusinessRuleException(
                    "Bed " + bed.getLabel() + " is not in the target ward");
        }
        return bed;
    }

    private void releaseCurrentBed(Admission admission) {
        if (admission.getBedUid() == null) return;
        bedRepository.findByUid(admission.getBedUid()).ifPresent(Bed::release);
    }

    @Transactional(readOnly = true)
    public AdmissionDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public List<AdmissionSummary> recentForPatient(String patientUid) {
        return admissionRepository.findTop10ByPatientUidOrderByAdmittedAtDesc(patientUid).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * The nurse worklist: currently-ADMITTED patients (optionally filtered to a
     * ward) whose nursing chart, vitals and consumables are open for entry.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdmissionSummary> nurseWorklist(String wardUid, Pageable pageable) {
        return PageResponse.from(
                admissionRepository.nurseWorklist(emptyToNull(wardUid), pageable)
                        .map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdmissionSummary> search(String query, AdmissionStatus status,
                                                 String wardUid, String patientUid, Pageable pageable) {
        return PageResponse.from(
                admissionRepository.search(
                        query == null ? null : query.trim(),
                        status,
                        emptyToNull(wardUid),
                        emptyToNull(patientUid),
                        pageable)
                        .map(this::toSummary));
    }

    private Admission loadOrThrow(String uid) {
        return admissionRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + uid));
    }

    // ----- mapping -----------------------------------------------------------

    private AdmissionDto toDto(Admission a) {
        Patient patient = patientRepository.findByUid(a.getPatientUid()).orElse(null);
        Ward ward = wardRepository.findByUid(a.getWardUid()).orElse(null);
        User clinician = userRepository.findByUsername(a.getAdmittingClinicianUsername()).orElse(null);
        InsurancePlan plan = a.getInsurancePlanUid() == null
                ? null
                : insurancePlanRepository.findByUid(a.getInsurancePlanUid()).orElse(null);
        Consultation consultation = a.getConsultationUid() == null
                ? null
                : consultationRepository.findByUid(a.getConsultationUid()).orElse(null);

        return new AdmissionDto(
                a.getUid(),
                a.getAdmissionNo(),
                a.getPatientUid(),
                patient == null ? null : patient.getPatientNo(),
                patient == null ? null : patient.fullName(),
                a.getWardUid(),
                ward == null ? null : ward.getName(),
                a.getBedUid(),
                a.getBedLabel(),
                a.getAdmittingClinicianUsername(),
                clinician == null ? null : clinician.getFirstName() + " " + clinician.getLastName(),
                a.getStatus(),
                a.getPaymentType(),
                a.getInsurancePlanUid(),
                plan == null ? null : plan.getName(),
                a.getConsultationUid(),
                consultation == null ? null : consultation.getConsultationNo(),
                a.getAdmissionReason(),
                a.getAdmittedAt(),
                a.getDischargedAt(),
                a.getDischargeSummary(),
                a.getCancelledAt(),
                a.getCancelReason(),
                a.isBillsCleared(),
                a.getBillsClearedAt(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    private AdmissionSummary toSummary(Admission a) {
        Patient patient = patientRepository.findByUid(a.getPatientUid()).orElse(null);
        Ward ward = wardRepository.findByUid(a.getWardUid()).orElse(null);
        User clinician = userRepository.findByUsername(a.getAdmittingClinicianUsername()).orElse(null);
        return new AdmissionSummary(
                a.getUid(),
                a.getAdmissionNo(),
                a.getPatientUid(),
                patient == null ? null : patient.getPatientNo(),
                patient == null ? null : patient.fullName(),
                ward == null ? null : ward.getName(),
                a.getBedLabel(),
                clinician == null ? null : clinician.getFirstName() + " " + clinician.getLastName(),
                a.getStatus(),
                a.getAdmittedAt(),
                a.getDischargedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
