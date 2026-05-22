package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacyToPharmacyRORepository extends JpaRepository<PharmacyToPharmacyRO, Long> {

    Optional<PharmacyToPharmacyRO> findByUid(String uid);

    @Query("""
            SELECT r FROM PharmacyToPharmacyRO r
            WHERE (:search IS NULL OR :search = '' OR LOWER(r.roNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status                 IS NULL OR r.status                 = :status)
              AND (:requestingPharmacyUid  IS NULL OR r.requestingPharmacyUid  = :requestingPharmacyUid)
              AND (:deliveringPharmacyUid  IS NULL OR r.deliveringPharmacyUid  = :deliveringPharmacyUid)
            """)
    Page<PharmacyToPharmacyRO> search(@Param("search") String search,
                                      @Param("status") TransferDocStatus status,
                                      @Param("requestingPharmacyUid") String requestingPharmacyUid,
                                      @Param("deliveringPharmacyUid") String deliveringPharmacyUid,
                                      Pageable pageable);
}
