package com.otapp.hmis.engine.encounter.consultation.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.CancelConsultationRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationDto;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationSummary;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.StartConsultationRequest;
import com.otapp.hmis.engine.encounter.consultation.application.event.ConsultationBookingRequestedEvent;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus;
import com.otapp.hmis.engine.encounter.consultation.infrastructure.ConsultationNumberGenerator;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.patient.application.PatientService;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final ClinicRepository clinicRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final UserRepository userRepository;
    private final ConsultationNumberGenerator numberGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ConsultationDto book(StartConsultationRequest request) {
        Patient patient = patientRepository.findByUid(request.patientUid())
                .orElseThrow(() -> new NotFoundException("Patient not found: " + request.patientUid()));
        if (!patient.isActive()) {
            throw new BusinessRuleException("Cannot start a consultation for an inactive patient");
        }
        if (patient.getType() == com.otapp.hmis.engine.patient.domain.PatientType.OUTSIDER) {
            throw new BusinessRuleException(
                    "Patient is registered as OUTSIDER; convert to OUTPATIENT before booking a consultation");
        }
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

        // Sync gate — any registered listener (billing) may throw to abort the booking
        // (e.g. cash patient with an unpaid registration invoice).
        eventPublisher.publishEvent(new ConsultationBookingRequestedEvent(patient.getUid(), request.paymentType()));

        Consultation consultation = new Consultation(
                numberGenerator.next(),
                patient.getUid(),
                clinic.getUid(),
                clinician.getUsername(),
                request.paymentType(),
                planUid,
                emptyToNull(request.reason()));
        consultationRepository.save(consultation);
        patientService.touchLastVisit(patient.getUid());
        return toDto(consultation);
    }

    @Transactional
    public ConsultationDto start(String uid) {
        Consultation c = loadOrThrow(uid);
        c.start();
        return toDto(c);
    }

    @Transactional
    public ConsultationDto complete(String uid) {
        Consultation c = loadOrThrow(uid);
        c.complete();
        return toDto(c);
    }

    @Transactional
    public ConsultationDto cancel(String uid, CancelConsultationRequest request) {
        Consultation c = loadOrThrow(uid);
        c.cancel(emptyToNull(request == null ? null : request.reason()));
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

    private Consultation loadOrThrow(String uid) {
        return consultationRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + uid));
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
                c.getReason(),
                c.getBookedAt(),
                c.getStartedAt(),
                c.getCompletedAt(),
                c.getCancelledAt(),
                c.getCancelReason(),
                c.getCreatedAt(),
                c.getUpdatedAt());
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
                c.getBookedAt(),
                c.getStartedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
