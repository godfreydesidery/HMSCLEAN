package com.otapp.hmis.engine.transfer.pharmacystorereturn.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyStoreReturnLineRepository extends JpaRepository<PharmacyStoreReturnLine, Long> {

    Optional<PharmacyStoreReturnLine> findByUid(String uid);

    List<PharmacyStoreReturnLine> findAllByReturnUidOrderByCreatedAtAsc(String returnUid);

    void deleteAllByReturnUid(String returnUid);
}
