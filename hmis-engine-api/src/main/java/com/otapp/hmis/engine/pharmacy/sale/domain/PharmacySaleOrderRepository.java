package com.otapp.hmis.engine.pharmacy.sale.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacySaleOrderRepository extends JpaRepository<PharmacySaleOrder, Long> {

    Optional<PharmacySaleOrder> findByUid(String uid);

    @Query("""
            SELECT s FROM PharmacySaleOrder s
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(s.saleNo)        LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.customerName)  LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status      IS NULL OR s.status      = :status)
              AND (:pharmacyUid IS NULL OR s.pharmacyUid = :pharmacyUid)
              AND (:patientUid  IS NULL OR s.patientUid  = :patientUid)
            """)
    Page<PharmacySaleOrder> search(@Param("search") String search,
                                   @Param("status") PharmacySaleOrderStatus status,
                                   @Param("pharmacyUid") String pharmacyUid,
                                   @Param("patientUid") String patientUid,
                                   Pageable pageable);
}
