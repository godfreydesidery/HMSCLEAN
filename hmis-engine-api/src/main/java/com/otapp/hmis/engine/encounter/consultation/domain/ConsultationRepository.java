package com.otapp.hmis.engine.encounter.consultation.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByUid(String uid);

    List<Consultation> findTop10ByPatientUidOrderByBookedAtDesc(String patientUid);

    @Query("""
            SELECT COUNT(c) FROM Consultation c
            WHERE c.clinicianUsername = :clinician
              AND c.bookedAt >= :from
              AND c.bookedAt <  :to
            """)
    long countByClinicianInRange(@Param("clinician") String clinicianUsername,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);

    @Query("""
            SELECT c FROM Consultation c
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(c.consultationNo)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.patientUid)        LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.clinicianUsername) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status     IS NULL OR c.status            = :status)
              AND (:clinicUid  IS NULL OR c.clinicUid         = :clinicUid)
              AND (:patientUid IS NULL OR c.patientUid        = :patientUid)
              AND (:clinician  IS NULL OR c.clinicianUsername = :clinician)
            """)
    Page<Consultation> search(@Param("search")     String search,
                              @Param("status")     ConsultationStatus status,
                              @Param("clinicUid")  String clinicUid,
                              @Param("patientUid") String patientUid,
                              @Param("clinician")  String clinicianUsername,
                              Pageable pageable);
}
