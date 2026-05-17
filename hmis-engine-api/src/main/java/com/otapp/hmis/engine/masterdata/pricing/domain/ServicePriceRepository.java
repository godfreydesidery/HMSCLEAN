package com.otapp.hmis.engine.masterdata.pricing.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicePriceRepository extends JpaRepository<ServicePrice, Long> {

    Optional<ServicePrice> findByUid(String uid);

    Optional<ServicePrice> findByPlanUidAndKindAndServiceUid(String planUid, ServiceKind kind, String serviceUid);

    /**
     * Convenience finder for the cash / public price (planUid is null).
     */
    @Query("""
            SELECT p FROM ServicePrice p
            WHERE p.planUid IS NULL
              AND p.kind = :kind
              AND p.serviceUid = :serviceUid
            """)
    Optional<ServicePrice> findCashPrice(@Param("kind") ServiceKind kind,
                                         @Param("serviceUid") String serviceUid);

    @Query("""
            SELECT p FROM ServicePrice p
            WHERE (:planUid    IS NULL OR p.planUid    = :planUid)
              AND (:kind       IS NULL OR p.kind       = :kind)
              AND (:serviceUid IS NULL OR p.serviceUid = :serviceUid)
            """)
    Page<ServicePrice> search(@Param("planUid")    String planUid,
                              @Param("kind")       ServiceKind kind,
                              @Param("serviceUid") String serviceUid,
                              Pageable pageable);
}
