package com.otapp.hmis.engine.encounter.consultation.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.CancelConsultationRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationDto;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationSummary;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.StartConsultationRequest;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.consultation.application.event.ConsultationBookedEvent;
import com.otapp.hmis.engine.encounter.consultation.application.event.ConsultationCancelledEvent;
import com.otapp.hmis.engine.encounter.consultation.application.event.ConsultationSignedOutEvent;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationTransferDtos.AcceptTransferRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationTransferDtos.CancelTransferRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationTransferDtos.ConsultationTransferDto;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationTransfer;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationTransferRepository;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationTransferStatus;
import com.otapp.hmis.engine.encounter.consultation.infrastructure.ConsultationNumberGenerator;
import com.otapp.hmis.engine.iam.application.StaffDirectoryService;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.clinicstaff.application.ClinicStaffService;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.patient.application.PatientService;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationTransferRepository consultationTransferRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final ClinicRepository clinicRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final UserRepository userRepository;
    private final ClinicStaffService clinicStaffService;
    private final StaffDirectoryService staffDirectoryService;
    private final AdmissionRepository admissionRepository;
    private final ConsultationCloseService consultationCloseService;
    private final ConsultationNumberGenerator numberGenerator;
    private final ApplicationEventPublisher eventPublisher;

    private static final String CLINICIAN_ROLE = "CLINICIAN";

    /**
     * "Ongoing" consultation statuses (legacy PENDING / TRANSFERED / IN-PROCESS):
     * a patient with a consultation in any of these has an active encounter, so a
     * second booking and any type / payment-type change are blocked.
     */
    static final Set<ConsultationStatus> ACTIVE_CONSULTATION_STATES = EnumSet.of(
            ConsultationStatus.BOOKED, ConsultationStatus.IN_PROGRESS, ConsultationStatus.TRANSFERRED);

    @Transactional
    public ConsultationDto book(StartConsultationRequest request) {
        Patient patient = resolveBookablePatient(request.patientUid());
        Clinic clinic = clinicRepository.findByUid(request.clinicUid())
                .orElseThrow(() -> new NotFoundException("Clinic not found: " + request.clinicUid()));
        if (!clinic.isActive()) {
            throw new BusinessRuleException("Clinic is not active: " + clinic.getName());
        }
        User clinician = userRepository.findByUsername(request.clinicianUsername())
                .orElseThrow(() -> new NotFoundException("Clinician not found: " + request.clinicianUsername()));
        if (!clinician.isEnabled()) {
            throw new BusinessRuleException("Clinician account is disabled");
        }
        requireClinicianOfClinic(clinician, clinic);

        boolean needsPlan = request.paymentType() == PaymentType.INSURANCE
                || request.paymentType() == PaymentType.MIXED;
        String planUid = (request.insurancePlanUid() == null || request.insurancePlanUid().isBlank())
                ? null : request.insurancePlanUid();
        if (needsPlan && planUid == null) {
            throw new BusinessRuleException("Insurance plan is required for payment type " + request.paymentType());
        }
        if (planUid != null) {
            insurancePlanRepository.findByUid(planUid)
                    .orElseThrow(() -> new NotFoundException("Insurance plan not found: " + planUid));
        }

        // Optional follow-up linkage — must reference an existing consultation for the same patient.
        String followUpOf = emptyToNull(request.followUpOfConsultationUid());
        if (followUpOf != null) {
            Consultation source = consultationRepository.findByUid(followUpOf)
                    .orElseThrow(() -> new NotFoundException("Source consultation not found: " + followUpOf));
            if (!source.getPatientUid().equals(patient.getUid())) {
                throw new BusinessRuleException("Follow-up source consultation belongs to a different patient");
            }
        }

        Consultation consultation = new Consultation(
                numberGenerator.next(),
                patient.getUid(),
                clinic.getUid(),
                clinician.getUsername(),
                request.paymentType(),
                planUid,
                emptyToNull(request.reason()));
        consultation.setFollowUpOfConsultationUid(followUpOf);
        consultationRepository.save(consultation);
        patientService.touchLastVisit(patient.getUid());

        // Hand off to billing (after-commit) to seed the consultation-fee invoice —
        // the legacy "send to doctor creates the consultation bill" step.
        eventPublisher.publishEvent(new ConsultationBookedEvent(
                consultation.getUid(), patient.getUid(), consultation.getPaymentType(),
                consultation.getInsurancePlanUid(), followUpOf != null));
        return toDto(consultation);
    }

    /**
     * Phase 1 — the treating doctor raises a PENDING transfer to another clinic
     * (OPC-1, legacy two-phase hand-off). No receiving clinician is chosen here;
     * reception picks the patient up later and books the receiving consultation.
     * The source consultation closes as TRANSFERRED (no receiver yet). Gates:
     * source must be IN_PROGRESS, the patient may not already have a pending
     * transfer, and the target clinic must exist, be active and differ from the
     * source clinic.
     */
    @Transactional
    public ConsultationTransferDto transfer(
            String uid,
            com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.TransferConsultationRequest request) {
        Consultation source = loadOrThrow(uid);
        if (source.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Not an active consultation (current: " + source.getStatus() + ")");
        }
        if (consultationTransferRepository.existsByPatientUidAndStatus(
                source.getPatientUid(), ConsultationTransferStatus.PENDING)) {
            throw new BusinessRuleException("The patient already has a pending transfer");
        }
        Clinic targetClinic = clinicRepository.findByUid(request.targetClinicUid())
                .orElseThrow(() -> new NotFoundException("Target clinic not found: " + request.targetClinicUid()));
        if (!targetClinic.isActive()) {
            throw new BusinessRuleException("Target clinic is not active: " + targetClinic.getName());
        }
        if (targetClinic.getUid().equals(source.getClinicUid())) {
            throw new BusinessRuleException("Cannot transfer to the same clinic");
        }
        // TODO OPC-1: legacy also blocks on un-acted PENDING orders / prescriptions
        // on the source consultation — skipped for now to avoid coupling to the
        // orders / prescription internals.

        ConsultationTransfer transferReq = new ConsultationTransfer(
                source.getUid(),
                source.getPatientUid(),
                targetClinic.getUid(),
                emptyToNull(request.reason()));
        consultationTransferRepository.save(transferReq);

        source.markTransferredPending(emptyToNull(request.reason()));
        return toTransferDto(transferReq);
    }

    /**
     * The receiving queue (OPC-1) — pending transfers for reception to pick up,
     * newest first. Defaults to PENDING.
     */
    @Transactional(readOnly = true)
    public PageResponse<ConsultationTransferDto> transferQueue(ConsultationTransferStatus status, Pageable pageable) {
        ConsultationTransferStatus effective = status == null ? ConsultationTransferStatus.PENDING : status;
        return PageResponse.from(
                consultationTransferRepository.findByStatusOrderByCreatedAtDesc(effective, pageable)
                        .map(this::toTransferDto));
    }

    /**
     * Phase 2 — reception accepts a pending transfer and books the receiving
     * consultation (OPC-1). The chosen clinician must hold the CLINICIAN role and
     * be affiliated with the transfer's target clinic. A FRESH consultation is
     * created at the target clinic + chosen clinician, inheriting payment type /
     * insurance plan / reason and the fee-settled state from the source (a
     * transfer does not re-charge the patient). No ConsultationBookedEvent is
     * published — re-seeding a fee invoice would double-charge.
     */
    @Transactional
    public ConsultationDto acceptTransfer(String transferUid, AcceptTransferRequest request) {
        ConsultationTransfer transferReq = loadTransferOrThrow(transferUid);
        if (transferReq.getStatus() != ConsultationTransferStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only a PENDING transfer can be accepted (current: " + transferReq.getStatus() + ")");
        }
        Consultation source = loadOrThrow(transferReq.getSourceConsultationUid());
        Clinic targetClinic = clinicRepository.findByUid(transferReq.getTargetClinicUid())
                .orElseThrow(() -> new NotFoundException("Target clinic not found: " + transferReq.getTargetClinicUid()));
        if (!targetClinic.isActive()) {
            throw new BusinessRuleException("Target clinic is not active: " + targetClinic.getName());
        }
        User clinician = userRepository.findByUsername(request.clinicianUsername())
                .orElseThrow(() -> new NotFoundException("Clinician not found: " + request.clinicianUsername()));
        if (!clinician.isEnabled()) {
            throw new BusinessRuleException("Clinician account is disabled");
        }
        requireClinicianOfClinic(clinician, targetClinic);

        Consultation receiver = new Consultation(
                numberGenerator.next(),
                source.getPatientUid(),
                targetClinic.getUid(),
                clinician.getUsername(),
                source.getPaymentType(),
                source.getInsurancePlanUid(),
                source.getReason());
        receiver.setTransferredFromConsultationUid(source.getUid());
        // Inherit the fee-settled state — a transfer keeps the original
        // consultation bill; the receiving clinic does not re-charge.
        if (source.isFeeSettled()) {
            receiver.markFeeSettled();
        }
        consultationRepository.save(receiver);

        // Fill in the audit link on the (already TRANSFERRED) source and complete
        // the transfer.
        source.linkTransferTarget(receiver.getUid());
        transferReq.complete(receiver.getUid());
        return toDto(receiver);
    }

    /**
     * Revert a pending transfer (OPC-1) — the initiating doctor cancelled before
     * pickup. The transfer is CANCELLED and the source consultation returns to
     * IN_PROGRESS. Gates: transfer must be PENDING and source TRANSFERRED.
     */
    @Transactional
    public ConsultationDto cancelTransfer(String transferUid, CancelTransferRequest request) {
        ConsultationTransfer transferReq = loadTransferOrThrow(transferUid);
        if (transferReq.getStatus() != ConsultationTransferStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only a PENDING transfer can be cancelled (current: " + transferReq.getStatus() + ")");
        }
        Consultation source = loadOrThrow(transferReq.getSourceConsultationUid());
        if (source.getStatus() != ConsultationStatus.TRANSFERRED) {
            throw new BusinessRuleException(
                    "Source consultation is not in a transferred state (current: " + source.getStatus() + ")");
        }
        transferReq.cancel(emptyToNull(request == null ? null : request.reason()));
        source.revertTransfer();
        return toDto(source);
    }

    @Transactional
    public ConsultationDto start(String uid) {
        Consultation c = loadOrThrow(uid);
        // Legacy gate: the doctor cannot open a CASH consultation until its
        // fee is settled. Non-CASH (insurance / corporate) is treated as
        // COVERED. Follow-up / plan-waived consultations are settled at booking.
        if (c.getPaymentType() == PaymentType.CASH && !c.isFeeSettled()) {
            throw new BusinessRuleException(
                    "Consultation fee not settled — the cashier must collect the consultation fee "
                    + "before the doctor can open this consultation");
        }
        c.start();
        return toDto(c);
    }

    /** Idempotent — flips the consultation-fee gate. Called by the billing settlement dispatcher. */
    @Transactional
    public void markFeeSettled(String consultationUid) {
        consultationRepository.findByUid(consultationUid).ifPresent(Consultation::markFeeSettled);
    }

    @Transactional
    public ConsultationDto complete(String uid) {
        Consultation c = loadOrThrow(uid);
        c.complete();
        // Legacy free_consultation cascade (step 5): cancel every UNPAID downstream
        // lab/rad/proc order + prescription in-transaction (encounter-side); PAID
        // items are left intact. The billing-side voids the unpaid invoice lines
        // after-commit via ConsultationSignedOutEvent (billing depends on encounter).
        consultationCloseService.cancelUnsettledDownstream(c.getUid());
        eventPublisher.publishEvent(new ConsultationSignedOutEvent(c.getUid(), c.getPatientUid()));
        return toDto(c);
    }

    /**
     * Close an open consultation as DECEASED — patient died during the encounter.
     * Encounter-internal; called by the closure-plan approval when a DECEASED plan
     * keyed to this consultation is approved. Runs the same unpaid-downstream
     * cancel + sign-out billing cascade as a normal sign-out (legacy free close).
     */
    @Transactional
    public ConsultationDto closeAsDeceased(String uid) {
        Consultation c = loadOrThrow(uid);
        c.closeAsDeceased();
        consultationCloseService.cancelUnsettledDownstream(c.getUid());
        eventPublisher.publishEvent(new ConsultationSignedOutEvent(c.getUid(), c.getPatientUid()));
        return toDto(c);
    }

    /**
     * Close an open consultation as REFERRED — patient referred out to an external
     * facility. Encounter-internal; called by the closure-plan approval when a
     * REFERRAL plan keyed to this consultation is approved. Runs the same
     * unpaid-downstream cancel + sign-out billing cascade as a normal sign-out.
     */
    @Transactional
    public ConsultationDto closeAsReferred(String uid) {
        Consultation c = loadOrThrow(uid);
        c.closeAsReferred();
        consultationCloseService.cancelUnsettledDownstream(c.getUid());
        eventPublisher.publishEvent(new ConsultationSignedOutEvent(c.getUid(), c.getPatientUid()));
        return toDto(c);
    }

    @Transactional
    public ConsultationDto cancel(String uid, CancelConsultationRequest request) {
        Consultation c = loadOrThrow(uid);
        c.cancel(emptyToNull(request == null ? null : request.reason()));
        // Legacy cancel_consultation cascade (step 4): the consultation-fee invoice
        // is voided, any received payment refunded, and a credit note raised. That
        // runs billing-side after-commit via ConsultationCancelledEvent — the
        // encounter module never imports billing.
        eventPublisher.publishEvent(new ConsultationCancelledEvent(c.getUid(), c.getPatientUid()));
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public ConsultationDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public List<ConsultationSummary> recentForPatient(String patientUid) {
        return consultationRepository.findTop10ByPatientUidOrderByBookedAtDesc(patientUid).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ConsultationSummary> search(String query, ConsultationStatus status,
                                                    String clinicUid, String patientUid,
                                                    String clinicianUsername, Pageable pageable) {
        return PageResponse.from(
                consultationRepository.search(
                        query == null ? null : query.trim(),
                        status,
                        emptyToNull(clinicUid),
                        emptyToNull(patientUid),
                        emptyToNull(clinicianUsername),
                        pageable)
                        .map(this::toSummary));
    }

    /**
     * The doctor's "from reception" queue: their BOOKED consultations whose
     * fee is settled (CASH paid, or non-CASH treated as COVERED), oldest
     * first. Unpaid CASH consultations are hidden until the cashier collects.
     */
    @Transactional(readOnly = true)
    public PageResponse<ConsultationSummary> receptionQueue(Pageable pageable) {
        return PageResponse.from(
                consultationRepository.findReceptionQueueFor(currentUsername(), pageable)
                        .map(this::toSummary));
    }

    /**
     * The signed-in clinician's own open consultations (OPC-4): their active
     * encounters — BOOKED, IN_PROGRESS or TRANSFERRED — newest first. Scoped to
     * {@link #currentUsername()} exactly like the reception queue.
     */
    @Transactional(readOnly = true)
    public PageResponse<ConsultationSummary> myOpenConsultations(Pageable pageable) {
        return PageResponse.from(
                consultationRepository.findMyOpenFor(currentUsername(), pageable)
                        .map(this::toSummary));
    }

    private Consultation loadOrThrow(String uid) {
        return consultationRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + uid));
    }

    private ConsultationTransfer loadTransferOrThrow(String uid) {
        return consultationTransferRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Consultation transfer not found: " + uid));
    }

    /**
     * Load the patient and enforce every booking pre-condition (legacy
     * {@code do_consultation} gates): not deceased, active, OUTPATIENT routing,
     * no active admission, no ongoing consultation.
     */
    private Patient resolveBookablePatient(String patientUid) {
        Patient patient = patientRepository.findByUid(patientUid)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientUid));
        if (patient.isDeceased()) {
            throw new BusinessRuleException("Patient is recorded as deceased and cannot be booked for a consultation");
        }
        if (!patient.isActive()) {
            throw new BusinessRuleException("Cannot start a consultation for an inactive patient");
        }
        if (patient.getType() == com.otapp.hmis.engine.patient.domain.PatientType.OUTSIDER) {
            throw new BusinessRuleException(
                    "Patient is registered as OUTSIDER; convert to OUTPATIENT before booking a consultation");
        }
        // Legacy do_consultation gate: refuse a new consultation while the patient
        // has an active admission — incl. deposit-pending (AWAITING_DEPOSIT), which
        // legacy's PENDING admission also blocked. "the patient has an active admission".
        if (admissionRepository.existsByPatientUidAndStatusIn(patient.getUid(), AdmissionStatus.ACTIVE)) {
            throw new BusinessRuleException("The patient has an active admission");
        }
        // Legacy do_consultation gate: refuse a new consultation while the patient
        // already has an ongoing one — "wait for the patient to be released".
        if (consultationRepository.countByPatientUidAndStatusIn(patient.getUid(), ACTIVE_CONSULTATION_STATES) > 0) {
            throw new BusinessRuleException(
                    "The patient already has an active consultation; wait for the patient to be released");
        }
        return patient;
    }

    /**
     * Legacy fidelity gate: a consultation may only be routed to a clinician
     * who holds the {@code CLINICIAN} role <em>and</em> is affiliated with the
     * chosen clinic ({@code Clinician.clinics}). Defense-in-depth + the
     * enforceable booking rule.
     */
    private void requireClinicianOfClinic(User clinician, Clinic clinic) {
        if (!staffDirectoryService.isUserInRole(clinician.getUsername(), CLINICIAN_ROLE)) {
            throw new BusinessRuleException("User " + clinician.getUsername() + " is not a clinician");
        }
        if (!clinicStaffService.isAssigned(clinic.getUid(), clinician.getUsername())) {
            throw new BusinessRuleException(
                    "Clinician " + clinician.getUsername() + " is not assigned to clinic " + clinic.getName());
        }
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user required");
        }
        return auth.getName();
    }

    // ----- mapping -----------------------------------------------------------

    private ConsultationDto toDto(Consultation c) {
        Patient patient = patientRepository.findByUid(c.getPatientUid()).orElse(null);
        Clinic clinic = clinicRepository.findByUid(c.getClinicUid()).orElse(null);
        User clinician = userRepository.findByUsername(c.getClinicianUsername()).orElse(null);
        InsurancePlan plan = c.getInsurancePlanUid() == null
                ? null
                : insurancePlanRepository.findByUid(c.getInsurancePlanUid()).orElse(null);

        return new ConsultationDto(
                c.getUid(),
                c.getConsultationNo(),
                c.getPatientUid(),
                patient == null ? null : patient.getPatientNo(),
                patient == null ? null : patient.fullName(),
                c.getClinicUid(),
                clinic == null ? null : clinic.getName(),
                c.getClinicianUsername(),
                clinician == null ? null : clinician.getFirstName() + " " + clinician.getLastName(),
                c.getStatus(),
                c.getPaymentType(),
                c.getInsurancePlanUid(),
                plan == null ? null : plan.getName(),
                c.isFeeSettled(),
                c.getStatus() == ConsultationStatus.IN_PROGRESS,
                c.getReason(),
                c.getBookedAt(),
                c.getStartedAt(),
                c.getCompletedAt(),
                c.getCancelledAt(),
                c.getCancelReason(),
                c.getFollowUpOfConsultationUid(),
                c.getTransferredToConsultationUid(),
                c.getTransferredFromConsultationUid(),
                c.getTransferReason(),
                c.getTransferredAt(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }

    private ConsultationTransferDto toTransferDto(ConsultationTransfer t) {
        Patient patient = patientRepository.findByUid(t.getPatientUid()).orElse(null);
        Clinic targetClinic = clinicRepository.findByUid(t.getTargetClinicUid()).orElse(null);
        Consultation source = consultationRepository.findByUid(t.getSourceConsultationUid()).orElse(null);
        return new ConsultationTransferDto(
                t.getId(),
                t.getUid(),
                t.getSourceConsultationUid(),
                source == null ? null : source.getConsultationNo(),
                t.getPatientUid(),
                patient == null ? null : patient.getPatientNo(),
                patient == null ? null : patient.fullName(),
                t.getTargetClinicUid(),
                targetClinic == null ? null : targetClinic.getName(),
                t.getStatus(),
                t.getReason(),
                t.getCreatedConsultationUid(),
                t.getCancelReason(),
                t.getCompletedAt(),
                t.getCancelledAt(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    private ConsultationSummary toSummary(Consultation c) {
        Patient patient = patientRepository.findByUid(c.getPatientUid()).orElse(null);
        Clinic clinic = clinicRepository.findByUid(c.getClinicUid()).orElse(null);
        User clinician = userRepository.findByUsername(c.getClinicianUsername()).orElse(null);
        return new ConsultationSummary(
                c.getUid(),
                c.getConsultationNo(),
                c.getPatientUid(),
                patient == null ? null : patient.getPatientNo(),
                patient == null ? null : patient.fullName(),
                clinic == null ? null : clinic.getName(),
                clinician == null ? null : clinician.getFirstName() + " " + clinician.getLastName(),
                c.getStatus(),
                c.getPaymentType(),
                c.isFeeSettled(),
                c.getBookedAt(),
                c.getStartedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
