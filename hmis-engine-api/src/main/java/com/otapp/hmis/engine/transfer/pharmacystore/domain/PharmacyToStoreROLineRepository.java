package com.otapp.hmis.engine.transfer.pharmacystore.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyToStoreROLineRepository extends JpaRepository<PharmacyToStoreROLine, Long> {

    Optional<PharmacyToStoreROLine> findByUid(String uid);

    List<PharmacyToStoreROLine> findAllByRoUidOrderByCreatedAtAsc(String roUid);
}
