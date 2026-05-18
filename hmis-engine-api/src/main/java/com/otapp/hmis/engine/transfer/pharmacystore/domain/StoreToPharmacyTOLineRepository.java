package com.otapp.hmis.engine.transfer.pharmacystore.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreToPharmacyTOLineRepository extends JpaRepository<StoreToPharmacyTOLine, Long> {

    Optional<StoreToPharmacyTOLine> findByUid(String uid);

    List<StoreToPharmacyTOLine> findAllByToUidOrderByCreatedAtAsc(String toUid);
}
