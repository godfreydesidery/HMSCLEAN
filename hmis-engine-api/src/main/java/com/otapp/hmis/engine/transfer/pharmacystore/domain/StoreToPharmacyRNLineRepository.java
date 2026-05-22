package com.otapp.hmis.engine.transfer.pharmacystore.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreToPharmacyRNLineRepository extends JpaRepository<StoreToPharmacyRNLine, Long> {

    List<StoreToPharmacyRNLine> findAllByRnUidOrderByCreatedAtAsc(String rnUid);
}
