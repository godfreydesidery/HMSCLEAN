package com.otapp.hmis.engine.encounter.order.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicalOrderRepository extends JpaRepository<ClinicalOrder, Long> {

    Optional<ClinicalOrder> findByUid(String uid);

    List<ClinicalOrder> findAllByConsultationUidOrderByRequestedAtDesc(String consultationUid);

    List<ClinicalOrder> findAllByConsultationUidAndKindOrderByRequestedAtDesc(String consultationUid, ClinicalOrderKind kind);

    /** All orders for a patient — across consultations and outsider direct orders. */
    List<ClinicalOrder> findAllByPatientUidOrderByRequestedAtDesc(String patientUid);

    /** Orders raised directly on a patient (consultation_uid IS NULL — the OUTSIDER pathway). */
    List<ClinicalOrder> findAllByPatientUidAndConsultationUidIsNullOrderByRequestedAtDesc(String patientUid);

    /**
     * Count orders of a given kind raised by a clinician (orders whose
     * createdBy audit field matches the clinician's username) in a time
     * range. Used by the HR clinician-performance roll-up.
     */
    @Query("""
            SELECT COUNT(o) FROM ClinicalOrder o
            WHERE o.kind = :kind
              AND o.createdBy = :clinician
              AND o.requestedAt >= :from
              AND o.requestedAt <  :to
            """)
    long countByKindAndCreatedByInRange(@Param("kind") ClinicalOrderKind kind,
                                        @Param("clinician") String clinicianUsername,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to);
}
