package com.otapp.hmis.engine.encounter.prescription.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByUid(String uid);

    List<Prescription> findAllByConsultationUidOrderByRequestedAtDesc(String consultationUid);
}
