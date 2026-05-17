package com.otapp.hmis.engine.encounter.admission.domain;

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
}
