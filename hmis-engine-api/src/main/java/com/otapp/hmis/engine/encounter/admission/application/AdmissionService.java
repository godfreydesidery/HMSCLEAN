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
import com.otapp.hmis.engine.encounter.admission.infrastructure.AdmissionNumberGenerator;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.masterdata.bed.domain.Bed;
import com.otapp.hmis.engine.masterdata.bed.domain.BedRepository;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.ward.domain.Ward;
import com.otapp.hmis.engine.masterdata.ward.domain.WardRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final PatientRepository patientRepository;
    private final com.otapp.hmis.engine.patient.application.PatientService patientService;
    private final WardRepository wardRepository;
    private final BedRepository bedRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;
    private final AdmissionNumberGenerator numberGenerator;

    @Transactional
    public AdmissionDto admit(AdmitPatientRequest request) {
        Patient patient = patientRepository.findByUid(request.patientUid())
                .orElseThrow(() -> new NotFoundException("Patient not found: " + request.patientUid()));
        if (!patient.isActive()) {
            throw new BusinessRuleException("Cannot admit an inactive patient");
        }
        if (admissionRepository.existsByPatientUidAndStatus(patient.getUid(), AdmissionStatus.ADMITTED)) {
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
        admissionRepository.save(admission);

        // Optional bed assignment — if a typed Bed is given, claim it
        // and overwrite the free-text label with the canonical Bed.label.
        String bedUid = emptyToNull(request.bedUid());
        if (bedUid != null) {
            Bed bed = loadBedInWard(bedUid, ward.getUid());
            bed.claim(admission.getUid());
            admission.setBedUid(bed.getUid());
            admission.setBedLabel(bed.getLabel());
        }
        patientService.touchLastVisit(patient.getUid());
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
        admission.discharge(emptyToNull(request == null ? null : request.summary()));
        releaseCurrentBed(admission);
        return toDto(admission);
    }

    @Transactional
    public AdmissionDto markDeceased(String uid, DischargeRequest request) {
        Admission admission = loadOrThrow(uid);
        admission.markDeceased(emptyToNull(request == null ? null : request.summary()));
        releaseCurrentBed(admission);
        return toDto(admission);
    }

    @Transactional
    public AdmissionDto transferOut(String uid, DischargeRequest request) {
        Admission admission = loadOrThrow(uid);
        admission.transferOut(emptyToNull(request == null ? null : request.summary()));
        releaseCurrentBed(admission);
        return toDto(admission);
    }

    @Transactional
    public AdmissionDto cancel(String uid, CancelAdmissionRequest request) {
        Admission admission = loadOrThrow(uid);
        admission.cancel(emptyToNull(request == null ? null : request.reason()));
        releaseCurrentBed(admission);
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
