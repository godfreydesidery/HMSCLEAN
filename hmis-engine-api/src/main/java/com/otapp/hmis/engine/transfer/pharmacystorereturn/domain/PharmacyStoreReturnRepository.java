package com.otapp.hmis.engine.transfer.pharmacystorereturn.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacyStoreReturnRepository extends JpaRepository<PharmacyStoreReturn, Long> {

    Optional<PharmacyStoreReturn> findByUid(String uid);

    @Query("""
            SELECT r FROM PharmacyStoreReturn r
            WHERE (:search IS NULL OR :search = '' OR LOWER(r.returnNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status      IS NULL OR r.status      = :status)
              AND (:pharmacyUid IS NULL OR r.pharmacyUid = :pharmacyUid)
              AND (:storeUid    IS NULL OR r.storeUid    = :storeUid)
            """)
    Page<PharmacyStoreReturn> search(@Param("search") String search,
                                     @Param("status") PharmacyStoreReturnStatus status,
                                     @Param("pharmacyUid") String pharmacyUid,
                                     @Param("storeUid") String storeUid,
                                     Pageable pageable);
}
