package com.otapp.hmis.engine.encounter.nursingchart.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareActivityEntryRepository extends JpaRepository<CareActivityEntry, Long> {

    Optional<CareActivityEntry> findByUid(String uid);

    List<CareActivityEntry> findByAdmissionUidOrderByRecordedAtDesc(String admissionUid);
}
