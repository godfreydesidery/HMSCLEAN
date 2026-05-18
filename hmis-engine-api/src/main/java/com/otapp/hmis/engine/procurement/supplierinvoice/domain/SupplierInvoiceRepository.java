package com.otapp.hmis.engine.procurement.supplierinvoice.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {

    Optional<SupplierInvoice> findByUid(String uid);

    boolean existsBySupplierUidAndSupplierInvoiceNo(String supplierUid, String supplierInvoiceNo);

    List<SupplierInvoice> findByOrderUidOrderByInvoiceDateDesc(String orderUid);

    @Query("""
            SELECT i FROM SupplierInvoice i
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(i.supplierInvoiceNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status      IS NULL OR i.status      = :status)
              AND (:supplierUid IS NULL OR i.supplierUid = :supplierUid)
              AND (:orderUid    IS NULL OR i.orderUid    = :orderUid)
            """)
    Page<SupplierInvoice> search(@Param("search") String search,
                                 @Param("status") SupplierInvoiceStatus status,
                                 @Param("supplierUid") String supplierUid,
                                 @Param("orderUid") String orderUid,
                                 Pageable pageable);
}
