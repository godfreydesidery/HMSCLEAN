package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import com.otapp.hmis.engine.transfer.common.domain.ReceiveNoteStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacyToPharmacyRNRepository extends JpaRepository<PharmacyToPharmacyRN, Long> {

    Optional<PharmacyToPharmacyRN> findByUid(String uid);

    boolean existsByToUidAndStatus(String toUid, ReceiveNoteStatus status);

    @Query("""
            SELECT r FROM PharmacyToPharmacyRN r
            WHERE (:search IS NULL OR :search = '' OR LOWER(r.rnNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status                 IS NULL OR r.status                 = :status)
              AND (:requestingPharmacyUid  IS NULL OR r.requestingPharmacyUid  = :requestingPharmacyUid)
              AND (:deliveringPharmacyUid  IS NULL OR r.deliveringPharmacyUid  = :deliveringPharmacyUid)
              AND (:toUid                  IS NULL OR r.toUid                  = :toUid)
            """)
    Page<PharmacyToPharmacyRN> search(@Param("search") String search,
                                      @Param("status") ReceiveNoteStatus status,
                                      @Param("requestingPharmacyUid") String requestingPharmacyUid,
                                      @Param("deliveringPharmacyUid") String deliveringPharmacyUid,
                                      @Param("toUid") String toUid,
                                      Pageable pageable);
}
