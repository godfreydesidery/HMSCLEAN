package com.otapp.hmis.engine.encounter.nursingchart.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionVitalsEntryRepository extends JpaRepository<AdmissionVitalsEntry, Long> {

    Optional<AdmissionVitalsEntry> findByUid(String uid);

    List<AdmissionVitalsEntry> findByAdmissionUidOrderByRecordedAtDesc(String admissionUid);
}
