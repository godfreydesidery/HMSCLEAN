package com.otapp.hmis.engine.encounter.discharge.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DischargePlanRepository extends JpaRepository<DischargePlan, Long> {

    Optional<DischargePlan> findByUid(String uid);

    /** One plan per admission (enforced by a partial unique index). */
    Optional<DischargePlan> findByAdmissionUid(String admissionUid);

    /** One plan per consultation (enforced by a partial unique index). */
    Optional<DischargePlan> findByConsultationUid(String consultationUid);

    /**
     * The closure worklist — every plan in a given status (PENDING for the
     * second-approver queue), oldest first, across BOTH subjects (admissions and
     * consultations). Derived queries are used so the enum params bind cleanly
     * (avoids the Hibernate enum-param inference quirk on JPQL {@code IS NULL}).
     */
    Page<DischargePlan> findByStatusOrderByAuthoredAtAsc(
            DischargePlanStatus status, Pageable pageable);

    /** Same worklist, scoped to one subject type (admission-only / consultation-only). */
    Page<DischargePlan> findByStatusAndSubjectTypeOrderByAuthoredAtAsc(
            DischargePlanStatus status, ClosureSubject subjectType, Pageable pageable);
}
