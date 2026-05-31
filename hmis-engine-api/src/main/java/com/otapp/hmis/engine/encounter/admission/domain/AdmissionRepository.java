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

    /**
     * Whether the patient has an admission in any of the given states. Callers use
     * {@link AdmissionStatus#ACTIVE} ({@code ADMITTED}, {@code AWAITING_DEPOSIT}) to
     * mean "has an in-flight admission" — the no-double-admit guard, the active-encounter
     * lock, and the consultation gate, since a deposit-pending admission still holds
     * the patient (legacy: a PENDING admission blocks a second admit).
     */
    boolean existsByPatientUidAndStatusIn(String patientUid, java.util.Collection<AdmissionStatus> statuses);

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

    /**
     * [wardUid, count] pairs for ACTIVE (ADMITTED + AWAITING_DEPOSIT) admissions — the
     * bed-occupancy fallback for wards with no Bed rows, mirroring the bed-table branch
     * which counts a RESERVED (deposit-held) bed as occupied, not free.
     */
    @Query("""
            SELECT a.wardUid, COUNT(a)
            FROM Admission a
            WHERE a.status IN (com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.ADMITTED, com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus.AWAITING_DEPOSIT)
            GROUP BY a.wardUid
            """)
    List<Object[]> countActiveByWard();

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
