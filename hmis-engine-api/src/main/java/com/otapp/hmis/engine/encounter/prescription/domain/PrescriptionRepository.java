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
     * The pharmacy dispensing queue — prescriptions awaiting pharmacy action,
     * optionally scoped by patient class (legacy outpatient / inpatient /
     * outsider lists). OUTSIDER = raised directly on the patient; INPATIENT =
     * consultation-bound for a patient with an active admission; OUTPATIENT =
     * consultation-bound, no active admission. Oldest first.
     */
    @Query("""
            SELECT p FROM Prescription p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:status IS NOT NULL OR p.status IN :activeStatuses)
              AND (:settledOnly = false OR p.settled = true)
              AND (
                    :scope IS NULL
                 OR (:scope = 'OUTSIDER'
                     AND p.consultationUid IS NULL)
                 OR (:scope = 'INPATIENT'
                     AND p.consultationUid IS NOT NULL
                     AND EXISTS (SELECT 1 FROM com.otapp.hmis.engine.encounter.admission.domain.Admission a
                                 WHERE a.patientUid = p.patientUid
                                   AND a.status = com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED))
                 OR (:scope = 'OUTPATIENT'
                     AND p.consultationUid IS NOT NULL
                     AND NOT EXISTS (SELECT 1 FROM com.otapp.hmis.engine.encounter.admission.domain.Admission a
                                     WHERE a.patientUid = p.patientUid
                                       AND a.status = com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED))
              )
            ORDER BY p.requestedAt ASC
            """)
    Page<Prescription> searchDispenseWorklist(@Param("status") PrescriptionStatus status,
                                              @Param("activeStatuses") Collection<PrescriptionStatus> activeStatuses,
                                              @Param("settledOnly") boolean settledOnly,
                                              @Param("scope") String scope,
                                              Pageable pageable);
}
