package com.otapp.hmis.engine.encounter.diagnosis.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationDiagnosisRepository extends JpaRepository<ConsultationDiagnosis, Long> {

    Optional<ConsultationDiagnosis> findByUid(String uid);

    List<ConsultationDiagnosis> findAllByConsultationUidOrderByKindAscPrimaryDiagnosisDescCreatedAtAsc(String consultationUid);

    boolean existsByConsultationUidAndKindAndDiagnosisTypeUid(String consultationUid, DiagnosisKind kind, String diagnosisTypeUid);
}
