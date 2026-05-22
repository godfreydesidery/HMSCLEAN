package com.otapp.hmis.engine.transfer.pharmacypharmacy.domain;

import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacyToPharmacyTORepository extends JpaRepository<PharmacyToPharmacyTO, Long> {

    Optional<PharmacyToPharmacyTO> findByUid(String uid);

    @Query("""
            SELECT t FROM PharmacyToPharmacyTO t
            WHERE (:search IS NULL OR :search = '' OR LOWER(t.toNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status                 IS NULL OR t.status                 = :status)
              AND (:requestingPharmacyUid  IS NULL OR t.requestingPharmacyUid  = :requestingPharmacyUid)
              AND (:deliveringPharmacyUid  IS NULL OR t.deliveringPharmacyUid  = :deliveringPharmacyUid)
              AND (:roUid                  IS NULL OR t.roUid                  = :roUid)
            """)
    Page<PharmacyToPharmacyTO> search(@Param("search") String search,
                                      @Param("status") TransferDocStatus status,
                                      @Param("requestingPharmacyUid") String requestingPharmacyUid,
                                      @Param("deliveringPharmacyUid") String deliveringPharmacyUid,
                                      @Param("roUid") String roUid,
                                      Pageable pageable);
}
