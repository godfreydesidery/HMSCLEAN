package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyToPharmacyTOBatchPickRepository
        extends JpaRepository<PharmacyToPharmacyTOBatchPick, Long> {

    List<PharmacyToPharmacyTOBatchPick> findAllByToLineUidOrderByCreatedAtAsc(String toLineUid);
}
