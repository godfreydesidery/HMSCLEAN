package com.otapp.hmis.engine.encounter.discharge.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DischargePlanRepository extends JpaRepository<DischargePlan, Long> {

    Optional<DischargePlan> findByUid(String uid);

    /** One plan per admission (enforced by unique constraint). */
    Optional<DischargePlan> findByAdmissionUid(String admissionUid);
}
