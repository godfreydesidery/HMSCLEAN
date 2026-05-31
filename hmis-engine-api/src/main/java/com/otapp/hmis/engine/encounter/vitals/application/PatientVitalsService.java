package com.otapp.hmis.engine.encounter.vitals.application;

import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.PatientVitalsDto;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.RecordVitalsRequest;
import com.otapp.hmis.engine.encounter.vitals.domain.PatientVitals;
import com.otapp.hmis.engine.encounter.vitals.domain.PatientVitalsRepository;
import com.otapp.hmis.engine.encounter.vitals.domain.VitalsCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientVitalsService {

    private final PatientVitalsRepository vitalsRepository;
    private final ConsultationRepository consultationRepository;

    @Transactional
    public PatientVitalsDto record(String consultationUid, RecordVitalsRequest request) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));

        PatientVitals vitals = new PatientVitals(consultation.getUid(), consultation.getPatientUid(), Instant.now());
        vitals.setTemperatureC(request.temperatureC());
        vitals.setPulseBpm(request.pulseBpm());
        vitals.setRespirationBpm(request.respirationBpm());
        vitals.setBloodPressureSystolic(request.bloodPressureSystolic());
        vitals.setBloodPressureDiastolic(request.bloodPressureDiastolic());
        vitals.setSpo2Percent(request.spo2Percent());
        vitals.setWeightKg(request.weightKg());
        vitals.setHeightCm(request.heightCm());
        // BMI / BSA: persist the clinician-supplied value (legacy behaviour);
        // derive server-side from weight & height only when not supplied.
        BigDecimal bmi = request.bmi() != null
                ? request.bmi() : VitalsCalculator.bmi(request.weightKg(), request.heightCm());
        BigDecimal bsa = request.bsa() != null
                ? request.bsa() : VitalsCalculator.bsaMosteller(request.weightKg(), request.heightCm());
        vitals.setBmi(bmi);
        vitals.setBsa(bsa);
        vitals.setBmiComment(emptyToNull(request.bmiComment()));
        vitals.setNotes(emptyToNull(request.notes()));
        vitalsRepository.save(vitals);
        return toDto(vitals);
    }

    @Transactional
    public void delete(String uid) {
        PatientVitals v = vitalsRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Vitals not found: " + uid));
        vitalsRepository.delete(v);
    }

    @Transactional(readOnly = true)
    public List<PatientVitalsDto> listForConsultation(String consultationUid) {
        return vitalsRepository.findAllByConsultationUidOrderByTakenAtDesc(consultationUid).stream()
                .map(PatientVitalsService::toDto)
                .toList();
    }

    private static PatientVitalsDto toDto(PatientVitals v) {
        return new PatientVitalsDto(
                v.getId(),
                v.getUid(),
                v.getConsultationUid(),
                v.getPatientUid(),
                v.getTakenAt(),
                v.getTemperatureC(),
                v.getPulseBpm(),
                v.getRespirationBpm(),
                v.getBloodPressureSystolic(),
                v.getBloodPressureDiastolic(),
                v.getSpo2Percent(),
                v.getWeightKg(),
                v.getHeightCm(),
                v.getBmi(),
                v.getBsa(),
                v.getBmiComment(),
                v.getNotes(),
                v.getCreatedAt(),
                v.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
