package com.otapp.hmis.engine.encounter.prescription.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByUid(String uid);

    List<Prescription> findAllByConsultationUidOrderByRequestedAtDesc(String consultationUid);

    /** Prescriptions raised directly on a patient (consultation_uid IS NULL — OUTSIDER pathway). */
    List<Prescription> findAllByPatientUidAndConsultationUidIsNullOrderByRequestedAtDesc(String patientUid);

    /**
     * Duplicate-drug-per-consultation guard (legacy existsByConsultationAndMedicine).
     * True when a non-withdrawn prescription already exists for this
     * consultation + medicine. {@code excluded} carries {CANCELLED, REJECTED}
     * so a withdrawn order can be re-prescribed (a deliberate refinement over
     * the legacy status-agnostic check).
     */
    boolean existsByConsultationUidAndMedicineUidAndStatusNotIn(
            String consultationUid, String medicineUid, Collection<PrescriptionStatus> excluded);

    /**
     * Backs the prescribing alerts (legacy findAllByPatientAndMedicineAndStatus
     * with status "GIVEN"; SOLD is the rewrite equivalent of GIVEN). Ordered by
     * the DISPENSE moment (newest first) — the legacy "GIVEN" timing is when the
     * medicine was actually handed over (dispensedAt), not when it was approved —
     * so the alert math reads the most-recent dispensed course off the head.
     */
    List<Prescription> findAllByPatientUidAndMedicineUidAndStatusOrderByDispensedAtDesc(
            String patientUid, String medicineUid, PrescriptionStatus status);

    /**
     * The pharmacy dispensing queue — prescriptions awaiting pharmacy action,
     * optionally scoped by patient class (legacy outpatient / inpatient /
     * outsider lists). OUTSIDER = raised directly on the patient; INPATIENT =
     * consultation-bound for a patient with an active admission; OUTPATIENT =
     * consultation-bound, no active admission. Oldest first.
     *
     * <p>When {@code hideUnpaid} is true (the legacy default) an ambulatory
     * script is only dispensable once its bill is settled (PAID / COVERED → the
     * {@code settled} flag), but an INPATIENT script stays visible regardless —
     * inpatient medication proceeds on the deposit/credit and clears at discharge.
     */
    @Query("""
            SELECT p FROM Prescription p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:status IS NOT NULL OR p.status IN :activeStatuses)
              AND (:hideUnpaid = false
                   OR p.settled = true
                   OR (p.consultationUid IS NOT NULL
                       AND EXISTS (SELECT 1 FROM com.otapp.hmis.engine.encounter.admission.domain.Admission a
                                   WHERE a.patientUid = p.patientUid
                                     AND a.status IN (com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED, com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.AWAITING_DEPOSIT))))
              AND (
                    :scope IS NULL
                 OR (:scope = 'OUTSIDER'
                     AND p.consultationUid IS NULL)
                 OR (:scope = 'INPATIENT'
                     AND p.consultationUid IS NOT NULL
                     AND EXISTS (SELECT 1 FROM com.otapp.hmis.engine.encounter.admission.domain.Admission a
                                 WHERE a.patientUid = p.patientUid
                                   AND a.status IN (com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED, com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.AWAITING_DEPOSIT)))
                 OR (:scope = 'OUTPATIENT'
                     AND p.consultationUid IS NOT NULL
                     AND NOT EXISTS (SELECT 1 FROM com.otapp.hmis.engine.encounter.admission.domain.Admission a
                                     WHERE a.patientUid = p.patientUid
                                       AND a.status IN (com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED, com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.AWAITING_DEPOSIT)))
              )
            ORDER BY p.requestedAt ASC
            """)
    Page<Prescription> searchDispenseWorklist(@Param("status") PrescriptionStatus status,
                                              @Param("activeStatuses") Collection<PrescriptionStatus> activeStatuses,
                                              @Param("hideUnpaid") boolean hideUnpaid,
                                              @Param("scope") String scope,
                                              Pageable pageable);
}
