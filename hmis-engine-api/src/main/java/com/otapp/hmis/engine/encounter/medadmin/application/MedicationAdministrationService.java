package com.otapp.hmis.engine.encounter.medadmin.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.medadmin.application.MedicationAdministrationDtos.MedicationAdministrationDto;
import com.otapp.hmis.engine.encounter.medadmin.application.MedicationAdministrationDtos.RecordAdministrationRequest;
import com.otapp.hmis.engine.encounter.medadmin.domain.MedicationAdministration;
import com.otapp.hmis.engine.encounter.medadmin.domain.MedicationAdministrationRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The nursing medication-administration record (MAR). Records each bedside dose
 * a nurse gives against an admission's prescription (PROCESS_MISMATCHES.md M15).
 */
@Service
@RequiredArgsConstructor
public class MedicationAdministrationService {

    private final MedicationAdministrationRepository repository;
    private final AdmissionRepository admissionRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;

    @Transactional
    public MedicationAdministrationDto record(String admissionUid, RecordAdministrationRequest request) {
        Admission admission = admissionRepository.findByUid(admissionUid)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + admissionUid));
        Prescription rx = prescriptionRepository.findByUid(request.prescriptionUid())
                .orElseThrow(() -> new NotFoundException("Prescription not found: " + request.prescriptionUid()));
        if (!rx.getPatientUid().equals(admission.getPatientUid())) {
            throw new BusinessRuleException("Prescription belongs to a different patient than the admission");
        }

        MedicationAdministration record = new MedicationAdministration(
                admission.getUid(),
                rx.getUid(),
                admission.getPatientUid(),
                rx.getMedicineUid(),
                request.doseGiven().trim(),
                emptyToNull(request.route()),
                emptyToNull(request.patientResponse()),
                emptyToNull(request.notes()),
                request.administeredAt() == null ? Instant.now() : request.administeredAt(),
                currentUsername());
        repository.save(record);
        return toDto(record);
    }

    @Transactional(readOnly = true)
    public List<MedicationAdministrationDto> listForAdmission(String admissionUid) {
        return repository.findAllByAdmissionUidOrderByAdministeredAtDesc(admissionUid).stream()
                .map(this::toDto)
                .toList();
    }

    private MedicationAdministrationDto toDto(MedicationAdministration a) {
        Prescription rx = prescriptionRepository.findByUid(a.getPrescriptionUid()).orElse(null);
        Medicine medicine = a.getMedicineUid() == null
                ? null
                : medicineRepository.findByUid(a.getMedicineUid()).orElse(null);
        return new MedicationAdministrationDto(
                a.getUid(),
                a.getAdmissionUid(),
                a.getPrescriptionUid(),
                rx == null ? null : rx.getPrescriptionNo(),
                a.getPatientUid(),
                a.getMedicineUid(),
                medicine == null ? null : medicine.getName(),
                a.getDoseGiven(),
                a.getRoute(),
                a.getPatientResponse(),
                a.getNotes(),
                a.getAdministeredAt(),
                a.getAdministeredByUsername(),
                a.getCreatedAt());
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
}
