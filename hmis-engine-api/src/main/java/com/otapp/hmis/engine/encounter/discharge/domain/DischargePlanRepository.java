package com.otapp.hmis.engine.encounter.discharge.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DischargePlanRepository extends JpaRepository<DischargePlan, Long> {

    Optional<DischargePlan> findByUid(String uid);

    /** One plan per admission (enforced by a partial unique index). */
    Optional<DischargePlan> findByAdmissionUid(String admissionUid);

    /** One plan per consultation (enforced by a partial unique index). */
    Optional<DischargePlan> findByConsultationUid(String consultationUid);
}
