package com.otapp.hmis.engine.encounter.admission.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    Optional<Admission> findByUid(String uid);

    List<Admission> findTop10ByPatientUidOrderByAdmittedAtDesc(String patientUid);

    boolean existsByPatientUidAndStatus(String patientUid, AdmissionStatus status);

    @Query("""
            SELECT COUNT(a) FROM Admission a
            WHERE a.admittingClinicianUsername = :clinician
              AND a.admittedAt >= :from
              AND a.admittedAt <  :to
            """)
    long countByAdmittingClinicianInRange(@Param("clinician") String clinicianUsername,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);

    @Query("""
            SELECT a FROM Admission a
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(a.admissionNo) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.patientUid)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.admittingClinicianUsername) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status     IS NULL OR a.status     = :status)
              AND (:wardUid    IS NULL OR a.wardUid    = :wardUid)
              AND (:patientUid IS NULL OR a.patientUid = :patientUid)
            """)
    Page<Admission> search(@Param("search")     String search,
                           @Param("status")     AdmissionStatus status,
                           @Param("wardUid")    String wardUid,
                           @Param("patientUid") String patientUid,
                           Pageable pageable);

    /**
     * The nurse worklist: currently-ADMITTED admissions, optionally filtered to
     * a ward, oldest admission first. Inpatient nursing work is admission-scoped.
     */
    @Query("""
            SELECT a FROM Admission a
            WHERE a.status = com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED
              AND (:wardUid IS NULL OR a.wardUid = :wardUid)
            ORDER BY a.admittedAt ASC
            """)
    Page<Admission> nurseWorklist(@Param("wardUid") String wardUid, Pageable pageable);

    /** UIDs of currently-ADMITTED admissions — drives the daily ward-day accrual job. */
    @Query("""
            SELECT a.uid FROM Admission a
            WHERE a.status = com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED
            """)
    List<String> findAdmittedUids();

    /** [wardUid, count] pairs for currently-ADMITTED admissions — drives the bed-occupancy report. */
    @Query("""
            SELECT a.wardUid, COUNT(a)
            FROM Admission a
            WHERE a.status = com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED
            GROUP BY a.wardUid
            """)
    List<Object[]> countCurrentlyAdmittedByWard();

    /** Admissions in a date range, optionally filtered by ward + status — the IPD register report. */
    @Query("""
            SELECT a FROM Admission a
            WHERE a.admittedAt >= :from
              AND a.admittedAt <  :to
              AND (:wardUid IS NULL OR a.wardUid = :wardUid)
              AND (:status  IS NULL OR a.status  = :status)
            ORDER BY a.admittedAt DESC
            """)
    List<Admission> ipdRegister(@Param("from") Instant from,
                                @Param("to") Instant to,
                                @Param("wardUid") String wardUid,
                                @Param("status") AdmissionStatus status);
}
