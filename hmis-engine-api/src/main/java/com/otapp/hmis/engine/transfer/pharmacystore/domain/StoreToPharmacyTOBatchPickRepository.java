package com.otapp.hmis.engine.transfer.pharmacystore.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreToPharmacyTOBatchPickRepository extends JpaRepository<StoreToPharmacyTOBatchPick, Long> {

    List<StoreToPharmacyTOBatchPick> findAllByToLineUidOrderByCreatedAtAsc(String toLineUid);
}
