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
     * Duplicate-order-type guard (legacy parity): no two orders of the same
     * service type within one encounter. Consultation path (OUTPATIENT/INPATIENT)
     * and outsider path (consultation_uid IS NULL). Matches the legacy {@code
     * existsByConsultationAndType} with no status filter.
     */
    boolean existsByConsultationUidAndKindAndServiceUid(String consultationUid, ClinicalOrderKind kind, String serviceUid);

    boolean existsByPatientUidAndConsultationUidIsNullAndKindAndServiceUid(String patientUid, ClinicalOrderKind kind, String serviceUid);

    /**
     * Cross-patient worklist for the Orders &amp; Results module — optional
     * kind / status / patient-class filters and the pay-before-service gate.
     * {@code scope} is passed as a String ('OUTPATIENT' / 'INPATIENT' /
     * 'OUTSIDER') to avoid Hibernate's enum value-mapping inference failure on
     * literal-only comparisons. Sort comes from the {@link Pageable}.
     *
     * <p>When {@code hideUnpaid} is true (the legacy default) an ambulatory
     * order is only worked once its bill is settled (PAID / COVERED → the
     * {@code settled} flag), but an INPATIENT order (consultation-bound, patient
     * has an active admission) stays visible regardless — inpatient care
     * proceeds on the deposit/credit and the bill clears at discharge.
     */
    @Query("""
            SELECT o FROM ClinicalOrder o
            WHERE (:kind   IS NULL OR o.kind = :kind)
              AND (:status IS NULL OR o.status = :status)
              AND (:hideUnpaid = false
                   OR o.settled = true
                   OR (o.consultationUid IS NOT NULL
                       AND EXISTS (SELECT 1 FROM com.otapp.hmis.engine.encounter.admission.domain.Admission a
                                   WHERE a.patientUid = o.patientUid
                                     AND a.status IN (com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED, com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.AWAITING_DEPOSIT))))
              AND (
                    :scope IS NULL
                 OR (:scope = 'OUTSIDER'
                     AND o.consultationUid IS NULL)
                 OR (:scope = 'INPATIENT'
                     AND o.consultationUid IS NOT NULL
                     AND EXISTS (SELECT 1 FROM com.otapp.hmis.engine.encounter.admission.domain.Admission a
                                 WHERE a.patientUid = o.patientUid
                                   AND a.status IN (com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED, com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.AWAITING_DEPOSIT)))
                 OR (:scope = 'OUTPATIENT'
                     AND o.consultationUid IS NOT NULL
                     AND NOT EXISTS (SELECT 1 FROM com.otapp.hmis.engine.encounter.admission.domain.Admission a
                                     WHERE a.patientUid = o.patientUid
                                       AND a.status IN (com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED, com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.AWAITING_DEPOSIT)))
              )
            """)
    Page<ClinicalOrder> searchWorklist(@Param("kind") ClinicalOrderKind kind,
                                       @Param("status") ClinicalOrderStatus status,
                                       @Param("scope") String scope,
                                       @Param("hideUnpaid") boolean hideUnpaid,
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
