package com.otapp.hmis.engine.encounter.vitals.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientVitalsRepository extends JpaRepository<PatientVitals, Long> {

    Optional<PatientVitals> findByUid(String uid);

    List<PatientVitals> findAllByConsultationUidOrderByTakenAtDesc(String consultationUid);
}
