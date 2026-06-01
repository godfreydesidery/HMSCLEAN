package com.otapp.hmis.engine.encounter.nursingchart.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FluidBalanceEntryRepository extends JpaRepository<FluidBalanceEntry, Long> {

    Optional<FluidBalanceEntry> findByUid(String uid);

    List<FluidBalanceEntry> findByAdmissionUidOrderByRecordedAtDesc(String admissionUid);
}
