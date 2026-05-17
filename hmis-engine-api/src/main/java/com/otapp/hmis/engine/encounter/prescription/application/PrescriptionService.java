package com.otapp.hmis.engine.encounter.prescription.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PatientType;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.CancelPrescriptionRequest;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.CreatePrescriptionRequest;
import com.otapp.hmis.engine.encounter.prescription.application.PrescriptionDtos.PrescriptionDto;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.infrastructure.PrescriptionNumberGenerator;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionNumberGenerator numberGenerator;

    @Transactional
    public PrescriptionDto prescribe(String consultationUid, CreatePrescriptionRequest request) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));
        Medicine medicine = medicineRepository.findByUid(request.medicineUid())
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + request.medicineUid()));

        Prescription prescription = new Prescription(
                numberGenerator.next(),
                consultation.getUid(),
                consultation.getPatientUid(),
                medicine.getUid(),
                request.dose().trim(),
                request.frequency().trim(),
                request.durationDays(),
                request.quantity(),
                emptyToNull(request.instructions()));
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
        Medicine medicine = medicineRepository.findByUid(request.medicineUid())
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + request.medicineUid()));

        Prescription prescription = new Prescription(
                numberGenerator.next(),
                null,
                patient.getUid(),
                medicine.getUid(),
                request.dose().trim(),
                request.frequency().trim(),
                request.durationDays(),
                request.quantity(),
                emptyToNull(request.instructions()));
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

    private Prescription loadOrThrow(String uid) {
        return prescriptionRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Prescription not found: " + uid));
    }

    private PrescriptionDto toDto(Prescription p) {
        Medicine m = medicineRepository.findByUid(p.getMedicineUid()).orElse(null);
        return toDto(p, m);
    }

    private static PrescriptionDto toDto(Prescription p, Medicine m) {
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
                p.getFrequency(),
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
}
