package com.otapp.hmis.engine.encounter.prescription.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientClassScope;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PatientType;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.CancelPrescriptionRequest;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.CreatePrescriptionRequest;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.PrescriptionDto;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.PrescriptionWorklistRow;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.encounter.prescription.infrastructure.PrescriptionNumberGenerator;
import com.otapp.hmis.engine.masterdata.administrationroute.domain.AdministrationRoute;
import com.otapp.hmis.engine.masterdata.administrationroute.domain.AdministrationRouteRepository;
import com.otapp.hmis.engine.masterdata.dosage.domain.Dosage;
import com.otapp.hmis.engine.masterdata.dosage.domain.DosageRepository;
import com.otapp.hmis.engine.masterdata.dosingfrequency.domain.DosingFrequency;
import com.otapp.hmis.engine.masterdata.dosingfrequency.domain.DosingFrequencyRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final java.util.Set<PrescriptionStatus> ACTIVE_PHARMACY_STATES = java.util.EnumSet.of(
            PrescriptionStatus.PENDING, PrescriptionStatus.ACCEPTED, PrescriptionStatus.HELD,
            PrescriptionStatus.VERIFIED, PrescriptionStatus.APPROVED);

    private final PrescriptionRepository prescriptionRepository;
    private final ConsultationRepository consultationRepository;
    private final AdmissionRepository admissionRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final DosageRepository dosageRepository;
    private final AdministrationRouteRepository routeRepository;
    private final DosingFrequencyRepository frequencyRepository;
    private final PrescriptionNumberGenerator numberGenerator;

    @Transactional
    public PrescriptionDto prescribe(String consultationUid, CreatePrescriptionRequest request) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));
        Medicine medicine = activeMedicine(request.medicineUid());

        ResolvedPicklists picks = resolvePicklists(request);
        Prescription prescription = new Prescription(
                numberGenerator.next(),
                consultation.getUid(),
                consultation.getPatientUid(),
                medicine.getUid(),
                picks.dose(),
                picks.frequency(),
                request.durationDays(),
                request.quantity(),
                emptyToNull(request.instructions()));
        applyPicklists(prescription, picks);
        prescriptionRepository.save(prescription);
        return toDto(prescription, medicine);
    }

    /**
     * Raise a prescription directly against an OUTSIDER (walk-in) patient,
     * bypassing consultation. Used for retail / OTC sales workflow.
     */
    @Transactional
    public PrescriptionDto prescribeForOutsider(String patientUid, CreatePrescriptionRequest request) {
        Patient patient = patientRepository.findByUid(patientUid)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientUid));
        if (!patient.isActive()) {
            throw new BusinessRuleException("Cannot prescribe for an inactive patient");
        }
        if (patient.getType() != PatientType.OUTSIDER) {
            throw new BusinessRuleException(
                    "Direct prescriptions are for OUTSIDER patients only; OUTPATIENT raises Rx inside a consultation");
        }
        Medicine medicine = activeMedicine(request.medicineUid());

        ResolvedPicklists picks = resolvePicklists(request);
        Prescription prescription = new Prescription(
                numberGenerator.next(),
                null,
                patient.getUid(),
                medicine.getUid(),
                picks.dose(),
                picks.frequency(),
                request.durationDays(),
                request.quantity(),
                emptyToNull(request.instructions()));
        applyPicklists(prescription, picks);
        prescriptionRepository.save(prescription);
        return toDto(prescription, medicine);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> listOutsiderForPatient(String patientUid) {
        return prescriptionRepository.findAllByPatientUidAndConsultationUidIsNullOrderByRequestedAtDesc(patientUid).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PrescriptionDto accept(String uid) {
        Prescription p = loadOrThrow(uid);
        p.accept();
        return toDto(p);
    }

    @Transactional
    public PrescriptionDto hold(String uid) {
        Prescription p = loadOrThrow(uid);
        p.hold();
        return toDto(p);
    }

    @Transactional
    public PrescriptionDto verify(String uid) {
        Prescription p = loadOrThrow(uid);
        p.verify();
        return toDto(p);
    }

    @Transactional
    public PrescriptionDto approve(String uid) {
        Prescription p = loadOrThrow(uid);
        p.approve();
        return toDto(p);
    }

    @Transactional
    public PrescriptionDto reject(String uid, CancelPrescriptionRequest request) {
        Prescription p = loadOrThrow(uid);
        p.reject(emptyToNull(request == null ? null : request.reason()));
        return toDto(p);
    }

    @Transactional
    public PrescriptionDto cancel(String uid, CancelPrescriptionRequest request) {
        Prescription p = loadOrThrow(uid);
        p.cancel(emptyToNull(request == null ? null : request.reason()));
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> listForConsultation(String consultationUid) {
        return prescriptionRepository.findAllByConsultationUidOrderByRequestedAtDesc(consultationUid).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * The pharmacy dispensing queue — prescriptions awaiting pharmacy action,
     * optionally scoped by patient class. When {@code status} is null the queue
     * shows all active pharmacy states (PENDING…APPROVED). {@code settledOnly}
     * defaults to false (medicines bill at point of dispense, not before).
     */
    @Transactional(readOnly = true)
    public PageResponse<PrescriptionWorklistRow> searchDispenseWorklist(
            PrescriptionStatus status, PatientClassScope scope, boolean settledOnly, Pageable pageable) {
        return PageResponse.from(
                prescriptionRepository.searchDispenseWorklist(
                                status, ACTIVE_PHARMACY_STATES, settledOnly,
                                scope == null ? null : scope.name(), pageable)
                        .map(this::toWorklistRow));
    }

    /** Idempotent — flips this prescription's settled flag. Called by the billing settlement dispatcher. */
    @Transactional
    public void markSettled(String prescriptionUid) {
        prescriptionRepository.findByUid(prescriptionUid).ifPresent(Prescription::markSettled);
    }

    private PrescriptionWorklistRow toWorklistRow(Prescription p) {
        Patient patient = patientRepository.findByUid(p.getPatientUid()).orElse(null);
        Medicine medicine = medicineRepository.findByUid(p.getMedicineUid()).orElse(null);
        return new PrescriptionWorklistRow(
                p.getUid(),
                p.getPrescriptionNo(),
                p.getPatientUid(),
                patient == null ? null : patient.getPatientNo(),
                patient == null ? null : patient.fullName(),
                resolvePatientClass(p),
                p.getConsultationUid(),
                medicine == null ? p.getMedicineUid() : medicine.getName(),
                p.getDose(),
                p.getFrequency(),
                p.getQuantity(),
                p.getStatus(),
                p.isSettled(),
                p.getRequestedAt());
    }

    private PatientClassScope resolvePatientClass(Prescription p) {
        if (p.getConsultationUid() == null) {
            return PatientClassScope.OUTSIDER;
        }
        return admissionRepository.existsByPatientUidAndStatus(p.getPatientUid(), AdmissionStatus.ADMITTED)
                ? PatientClassScope.INPATIENT
                : PatientClassScope.OUTPATIENT;
    }

    private Prescription loadOrThrow(String uid) {
        return prescriptionRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Prescription not found: " + uid));
    }

    private Medicine activeMedicine(String uid) {
        Medicine m = medicineRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
        if (!m.isActive()) {
            throw new BusinessRuleException("Medicine is not active: " + m.getName());
        }
        return m;
    }

    /**
     * Resolves the three picklist slots: if a uid is provided, look it up
     * and use the masterdata name as the canonical display value (so old
     * read paths that only look at the string columns still work). Free
     * text is the fallback when a uid is not given. Either way at least
     * one of {dose, dosageUid} and {frequency, frequencyUid} must be
     * supplied — Prescription's columns are NOT NULL.
     */
    private ResolvedPicklists resolvePicklists(CreatePrescriptionRequest req) {
        Dosage dosage = req.dosageUid() == null || req.dosageUid().isBlank()
                ? null
                : dosageRepository.findByUid(req.dosageUid())
                        .orElseThrow(() -> new NotFoundException("Dosage not found: " + req.dosageUid()));
        AdministrationRoute route = req.routeUid() == null || req.routeUid().isBlank()
                ? null
                : routeRepository.findByUid(req.routeUid())
                        .orElseThrow(() -> new NotFoundException("Administration route not found: " + req.routeUid()));
        DosingFrequency freq = req.frequencyUid() == null || req.frequencyUid().isBlank()
                ? null
                : frequencyRepository.findByUid(req.frequencyUid())
                        .orElseThrow(() -> new NotFoundException("Dosing frequency not found: " + req.frequencyUid()));

        String dose = dosage != null ? dosage.getName()
                : (req.dose() == null || req.dose().isBlank()
                        ? null : req.dose().trim());
        String frequency = freq != null ? freq.getName()
                : (req.frequency() == null || req.frequency().isBlank()
                        ? null : req.frequency().trim());
        String routeText = route != null ? route.getName()
                : (req.route() == null || req.route().isBlank()
                        ? null : req.route().trim());

        if (dose == null) {
            throw new BusinessRuleException("dose (free text) or dosageUid (picklist) is required");
        }
        if (frequency == null) {
            throw new BusinessRuleException("frequency (free text) or frequencyUid (picklist) is required");
        }
        return new ResolvedPicklists(dose, frequency, routeText,
                dosage == null ? null : dosage.getUid(),
                route  == null ? null : route.getUid(),
                freq   == null ? null : freq.getUid());
    }

    private static void applyPicklists(Prescription p, ResolvedPicklists picks) {
        p.setDosageUid(picks.dosageUid());
        p.setRoute(picks.route());
        p.setRouteUid(picks.routeUid());
        p.setFrequencyUid(picks.frequencyUid());
    }

    private PrescriptionDto toDto(Prescription p) {
        Medicine m = medicineRepository.findByUid(p.getMedicineUid()).orElse(null);
        return toDto(p, m);
    }

    private PrescriptionDto toDto(Prescription p, Medicine m) {
        Dosage dosage = p.getDosageUid() == null ? null
                : dosageRepository.findByUid(p.getDosageUid()).orElse(null);
        AdministrationRoute route = p.getRouteUid() == null ? null
                : routeRepository.findByUid(p.getRouteUid()).orElse(null);
        DosingFrequency freq = p.getFrequencyUid() == null ? null
                : frequencyRepository.findByUid(p.getFrequencyUid()).orElse(null);
        return new PrescriptionDto(
                p.getUid(),
                p.getPrescriptionNo(),
                p.getConsultationUid(),
                p.getPatientUid(),
                p.getMedicineUid(),
                m == null ? null : m.getCode(),
                m == null ? null : m.getName(),
                m == null ? null : m.getStrength(),
                m == null ? null : m.getForm(),
                p.getStatus(),
                p.getDose(),
                p.getDosageUid(),
                dosage == null ? null : dosage.getCode(),
                p.getRoute(),
                p.getRouteUid(),
                route == null ? null : route.getCode(),
                p.getFrequency(),
                p.getFrequencyUid(),
                freq == null ? null : freq.getCode(),
                freq == null ? null : freq.getTimesPerDay(),
                p.getDurationDays(),
                p.getQuantity(),
                p.getInstructions(),
                p.getRequestedAt(),
                p.getAcceptedAt(),
                p.getHeldAt(),
                p.getVerifiedAt(),
                p.getApprovedAt(),
                p.getDispensedAt(),
                p.getRejectedAt(),
                p.getRejectReason(),
                p.getCancelReason(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private record ResolvedPicklists(String dose, String frequency, String route,
                                     String dosageUid, String routeUid, String frequencyUid) {}
}
