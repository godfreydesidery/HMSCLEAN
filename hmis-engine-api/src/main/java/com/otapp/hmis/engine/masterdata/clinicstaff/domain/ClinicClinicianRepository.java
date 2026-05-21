package com.otapp.hmis.engine.masterdata.clinicstaff.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicClinicianRepository extends JpaRepository<ClinicClinician, Long> {

    Optional<ClinicClinician> findByUid(String uid);

    Optional<ClinicClinician> findByClinicUidAndUserUid(String clinicUid, String userUid);

    /** Backs the booking gate: is this clinician currently affiliated with the clinic? */
    boolean existsByClinicUidAndUsernameAndActiveTrue(String clinicUid, String username);

    /** Backs the clinic "Clinicians" listing. */
    List<ClinicClinician> findByClinicUidAndActiveTrueOrderByUsername(String clinicUid);
}
