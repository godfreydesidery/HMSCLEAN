package com.otapp.hmis.engine.procurement.order.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByUid(String uid);

    @Query("""
            SELECT po FROM PurchaseOrder po
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(po.orderNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status      IS NULL OR po.status      = :status)
              AND (:supplierUid IS NULL OR po.supplierUid = :supplierUid)
              AND (:storeUid    IS NULL OR po.storeUid    = :storeUid)
            """)
    Page<PurchaseOrder> search(@Param("search") String search,
                               @Param("status") PurchaseOrderStatus status,
                               @Param("supplierUid") String supplierUid,
                               @Param("storeUid") String storeUid,
                               Pageable pageable);
}
