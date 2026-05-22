package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyToPharmacyRNLineRepository extends JpaRepository<PharmacyToPharmacyRNLine, Long> {

    List<PharmacyToPharmacyRNLine> findAllByRnUidOrderByCreatedAtAsc(String rnUid);
}
