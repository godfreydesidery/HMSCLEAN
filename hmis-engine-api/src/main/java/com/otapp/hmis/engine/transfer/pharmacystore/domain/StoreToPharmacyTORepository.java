package com.otapp.hmis.engine.transfer.pharmacystore.domain;

import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreToPharmacyTORepository extends JpaRepository<StoreToPharmacyTO, Long> {

    Optional<StoreToPharmacyTO> findByUid(String uid);

    @Query("""
            SELECT t FROM StoreToPharmacyTO t
            WHERE (:search IS NULL OR :search = '' OR LOWER(t.toNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status      IS NULL OR t.status      = :status)
              AND (:pharmacyUid IS NULL OR t.pharmacyUid = :pharmacyUid)
              AND (:storeUid    IS NULL OR t.storeUid    = :storeUid)
              AND (:roUid       IS NULL OR t.roUid       = :roUid)
            """)
    Page<StoreToPharmacyTO> search(@Param("search") String search,
                                   @Param("status") TransferDocStatus status,
                                   @Param("pharmacyUid") String pharmacyUid,
                                   @Param("storeUid") String storeUid,
                                   @Param("roUid") String roUid,
                                   Pageable pageable);
}
