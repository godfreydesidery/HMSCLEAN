package com.otapp.hmis.engine.encounter.order.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicalOrderRepository extends JpaRepository<ClinicalOrder, Long> {

    Optional<ClinicalOrder> findByUid(String uid);

    /**
     * Cross-patient worklist for the Orders &amp; Results module — optional
     * kind / status filters. Sort comes from the {@link Pageable} (the service
     * defaults it to requestedAt desc) so no ORDER BY is baked in here.
     */
    @Query("""
            SELECT o FROM ClinicalOrder o
            WHERE (:kind   IS NULL OR o.kind = :kind)
              AND (:status IS NULL OR o.status = :status)
            """)
    Page<ClinicalOrder> searchWorklist(@Param("kind") ClinicalOrderKind kind,
                                       @Param("status") ClinicalOrderStatus status,
                                       Pageable pageable);

    List<ClinicalOrder> findAllByConsultationUidOrderByRequestedAtDesc(String consultationUid);

    List<ClinicalOrder> findAllByConsultationUidAndKindOrderByRequestedAtDesc(String consultationUid, ClinicalOrderKind kind);

    /** All orders for a patient — across consultations and outsider direct orders. */
    List<ClinicalOrder> findAllByPatientUidOrderByRequestedAtDesc(String patientUid);

    /**
     * Orders of one kind for a given service in a given status, oldest first.
     * Backs the lab-batch member picker (LAB_TEST + REQUESTED) — the
     * already-batched filter is applied in the service layer to keep this
     * module free of any lab-batch dependency.
     */
    List<ClinicalOrder> findAllByKindAndServiceUidAndStatusOrderByRequestedAtAsc(
            ClinicalOrderKind kind, String serviceUid, ClinicalOrderStatus status);

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
