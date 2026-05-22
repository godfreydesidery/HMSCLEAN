package com.otapp.hmis.engine.transfer.pharmacystore.domain;

import com.otapp.hmis.engine.transfer.common.domain.ReceiveNoteStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreToPharmacyRNRepository extends JpaRepository<StoreToPharmacyRN, Long> {

    Optional<StoreToPharmacyRN> findByUid(String uid);

    boolean existsByToUidAndStatus(String toUid, ReceiveNoteStatus status);

    @Query("""
            SELECT r FROM StoreToPharmacyRN r
            WHERE (:search IS NULL OR :search = '' OR LOWER(r.rnNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status      IS NULL OR r.status      = :status)
              AND (:pharmacyUid IS NULL OR r.pharmacyUid = :pharmacyUid)
              AND (:storeUid    IS NULL OR r.storeUid    = :storeUid)
              AND (:toUid       IS NULL OR r.toUid       = :toUid)
            """)
    Page<StoreToPharmacyRN> search(@Param("search") String search,
                                   @Param("status") ReceiveNoteStatus status,
                                   @Param("pharmacyUid") String pharmacyUid,
                                   @Param("storeUid") String storeUid,
                                   @Param("toUid") String toUid,
                                   Pageable pageable);
}
