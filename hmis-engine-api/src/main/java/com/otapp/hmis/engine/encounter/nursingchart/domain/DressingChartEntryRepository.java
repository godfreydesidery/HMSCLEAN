package com.otapp.hmis.engine.encounter.nursingchart.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DressingChartEntryRepository extends JpaRepository<DressingChartEntry, Long> {

    Optional<DressingChartEntry> findByUid(String uid);

    List<DressingChartEntry> findByAdmissionUidOrderByRecordedAtDesc(String admissionUid);
}
