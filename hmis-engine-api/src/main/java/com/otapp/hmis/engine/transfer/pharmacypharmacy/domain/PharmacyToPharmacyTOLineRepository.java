package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyToPharmacyTOLineRepository extends JpaRepository<PharmacyToPharmacyTOLine, Long> {

    Optional<PharmacyToPharmacyTOLine> findByUid(String uid);

    List<PharmacyToPharmacyTOLine> findAllByToUidOrderByCreatedAtAsc(String toUid);
}
