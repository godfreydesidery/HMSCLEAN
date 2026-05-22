package com.otapp.hmis.engine.transfer.pharmacystorereturn.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyStoreReturnBatchPickRepository
        extends JpaRepository<PharmacyStoreReturnBatchPick, Long> {

    List<PharmacyStoreReturnBatchPick> findAllByReturnLineUidOrderByCreatedAtAsc(String returnLineUid);
}
