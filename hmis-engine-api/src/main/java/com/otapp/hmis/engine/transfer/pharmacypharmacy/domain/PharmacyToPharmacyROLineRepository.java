package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyToPharmacyROLineRepository extends JpaRepository<PharmacyToPharmacyROLine, Long> {

    Optional<PharmacyToPharmacyROLine> findByUid(String uid);

    List<PharmacyToPharmacyROLine> findAllByRoUidOrderByCreatedAtAsc(String roUid);
}
