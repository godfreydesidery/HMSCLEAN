package com.otapp.hmis.engine.encounter.medadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {

    /** The MAR for an admission, newest dose first. */
    List<MedicationAdministration> findAllByAdmissionUidOrderByAdministeredAtDesc(String admissionUid);
}
