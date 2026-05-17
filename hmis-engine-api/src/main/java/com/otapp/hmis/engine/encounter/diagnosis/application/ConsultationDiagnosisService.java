package com.otapp.hmis.engine.encounter.diagnosis.application;

import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.diagnosis.application.ConsultationDiagnosisDtos.AddDiagnosisRequest;
import com.otapp.hmis.engine.encounter.diagnosis.application.ConsultationDiagnosisDtos.ConsultationDiagnosisDto;
import com.otapp.hmis.engine.encounter.diagnosis.domain.ConsultationDiagnosis;
import com.otapp.hmis.engine.encounter.diagnosis.domain.ConsultationDiagnosisRepository;
import com.otapp.hmis.engine.masterdata.diagnosis.domain.DiagnosisType;
import com.otapp.hmis.engine.masterdata.diagnosis.domain.DiagnosisTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationDiagnosisService {

    private final ConsultationDiagnosisRepository repo;
    private final ConsultationRepository consultationRepository;
    private final DiagnosisTypeRepository diagnosisTypeRepository;

    @Transactional
    public ConsultationDiagnosisDto add(String consultationUid, AddDiagnosisRequest request) {
        if (consultationRepository.findByUid(consultationUid).isEmpty()) {
            throw new NotFoundException("Consultation not found: " + consultationUid);
        }
        DiagnosisType type = diagnosisTypeRepository.findByUid(request.diagnosisTypeUid())
                .orElseThrow(() -> new NotFoundException("Diagnosis type not found: " + request.diagnosisTypeUid()));

        if (repo.existsByConsultationUidAndKindAndDiagnosisTypeUid(consultationUid, request.kind(), type.getUid())) {
            throw new ConflictException("This diagnosis is already recorded as " + request.kind());
        }

        ConsultationDiagnosis diagnosis = new ConsultationDiagnosis(
                consultationUid, type.getUid(), request.kind(),
                request.primaryDiagnosis(), emptyToNull(request.notes()));
        repo.save(diagnosis);
        return toDto(diagnosis, type);
    }

    @Transactional
    public void remove(String uid) {
        ConsultationDiagnosis d = repo.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Diagnosis entry not found: " + uid));
        repo.delete(d);
    }

    @Transactional(readOnly = true)
    public List<ConsultationDiagnosisDto> listForConsultation(String consultationUid) {
        return repo.findAllByConsultationUidOrderByKindAscPrimaryDiagnosisDescCreatedAtAsc(consultationUid).stream()
                .map(this::toDto)
                .toList();
    }

    private ConsultationDiagnosisDto toDto(ConsultationDiagnosis d) {
        DiagnosisType type = diagnosisTypeRepository.findByUid(d.getDiagnosisTypeUid()).orElse(null);
        return toDto(d, type);
    }

    private static ConsultationDiagnosisDto toDto(ConsultationDiagnosis d, DiagnosisType type) {
        return new ConsultationDiagnosisDto(
                d.getUid(),
                d.getConsultationUid(),
                d.getKind(),
                d.getDiagnosisTypeUid(),
                type == null ? null : type.getCode(),
                type == null ? null : type.getName(),
                d.isPrimaryDiagnosis(),
                d.getNotes(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
