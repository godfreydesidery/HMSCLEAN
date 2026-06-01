package com.otapp.hmis.engine.encounter.vitals.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.PatientVitalsDto;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.RecordVitalsRequest;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.VitalsWorklistRow;
import com.otapp.hmis.engine.encounter.vitals.domain.PatientVitals;
import com.otapp.hmis.engine.encounter.vitals.domain.PatientVitalsRepository;
import com.otapp.hmis.engine.encounter.vitals.domain.VitalsCalculator;
import com.otapp.hmis.engine.encounter.vitals.domain.VitalsStatus;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientVitalsService {

    private final PatientVitalsRepository vitalsRepository;
    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;

    /**
     * Nurse fill / save (legacy {@code save}): create-or-update the single open
     * (EMPTY / PENDING) vitals row for the consultation with the readings and
     * leave it PENDING — still editable until submitted. If the only existing
     * rows are SUBMITTED / ARCHIVED (locked), a fresh PENDING row is started so a
     * new triage reading can be captured.
     */
    @Transactional
    public PatientVitalsDto save(String consultationUid, RecordVitalsRequest request) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));

        PatientVitals vitals = vitalsRepository
                .findFirstByConsultationUidAndStatusInOrderByTakenAtDesc(
                        consultation.getUid(), EnumSet.of(VitalsStatus.EMPTY, VitalsStatus.PENDING))
                .orElseGet(() -> PatientVitals.empty(
                        consultation.getUid(), consultation.getPatientUid(), Instant.now()));

        vitals.setTakenAt(Instant.now());
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
        vitals.markFilled(); // EMPTY/PENDING → PENDING (guards a locked row)
        vitalsRepository.save(vitals);
        return toDto(vitals);
    }

    /** Nurse submit (legacy {@code submit}): PENDING → SUBMITTED; locks the set. */
    @Transactional
    public PatientVitalsDto submit(String vitalsUid) {
        PatientVitals vitals = loadOrThrow(vitalsUid);
        vitals.submit();
        vitalsRepository.save(vitals);
        return toDto(vitals);
    }

    /**
     * Doctor consume (legacy: copy into GeneralExamination + flag archived):
     * SUBMITTED → ARCHIVED. Acknowledges the captured set is taken into the exam.
     */
    @Transactional
    public PatientVitalsDto consume(String vitalsUid) {
        PatientVitals vitals = loadOrThrow(vitalsUid);
        vitals.consume();
        vitalsRepository.save(vitals);
        return toDto(vitals);
    }

    @Transactional
    public void delete(String uid) {
        PatientVitals v = loadOrThrow(uid);
        vitalsRepository.delete(v);
    }

    @Transactional(readOnly = true)
    public List<PatientVitalsDto> listForConsultation(String consultationUid) {
        return vitalsRepository.findAllByConsultationUidOrderByTakenAtDesc(consultationUid).stream()
                .map(PatientVitalsService::toDto)
                .toList();
    }

    /**
     * The OUTPATIENT nurse-triage worklist: fee-settled consultations still
     * BOOKED / IN_PROGRESS, enriched with the patient identity and the current
     * vitals status so the nurse can see who still needs vitals. Distinct from
     * the inpatient (admission-scoped) nurse worklist.
     */
    @Transactional(readOnly = true)
    public PageResponse<VitalsWorklistRow> nurseWorklist(Pageable pageable) {
        return PageResponse.from(
                consultationRepository.findOutpatientNurseWorklist(pageable).map(this::toWorklistRow));
    }

    private PatientVitals loadOrThrow(String uid) {
        return vitalsRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Vitals not found: " + uid));
    }

    private VitalsWorklistRow toWorklistRow(Consultation c) {
        Patient patient = patientRepository.findByUid(c.getPatientUid()).orElse(null);
        PatientVitals current = vitalsRepository
                .findFirstByConsultationUidOrderByTakenAtDesc(c.getUid()).orElse(null);
        return new VitalsWorklistRow(
                c.getId(),
                c.getUid(),
                c.getConsultationNo(),
                c.getPatientUid(),
                patient == null ? null : patient.getPatientNo(),
                patient == null ? null : patient.fullName(),
                c.getStatus(),
                c.isFeeSettled(),
                current == null ? null : current.getUid(),
                current == null ? null : current.getStatus(),
                c.getBookedAt(),
                c.getStartedAt());
    }

    private static PatientVitalsDto toDto(PatientVitals v) {
        return new PatientVitalsDto(
                v.getId(),
                v.getUid(),
                v.getConsultationUid(),
                v.getPatientUid(),
                v.getStatus(),
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
                v.getSubmittedAt(),
                v.getArchivedAt(),
                v.getCreatedAt(),
                v.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
