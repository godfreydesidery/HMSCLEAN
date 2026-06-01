package com.otapp.hmis.engine.patient.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUid(String uid);

    Optional<Patient> findByPatientNo(String patientNo);

    boolean existsByPatientNo(String patientNo);

    @Query("""
            SELECT p FROM Patient p
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(p.patientNo)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.firstName)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.lastName)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.phoneNo)      LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.nationalId)   LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.membershipNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active      IS NULL OR p.active      = :active)
              AND (:gender      IS NULL OR p.gender      = :gender)
              AND (:paymentType IS NULL OR p.paymentType = :paymentType)
            """)
    Page<Patient> search(@Param("search") String search,
                         @Param("active") Boolean active,
                         @Param("gender") Gender gender,
                         @Param("paymentType") PaymentType paymentType,
                         Pageable pageable);
}
