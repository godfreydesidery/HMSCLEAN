package com.otapp.hmis.engine.masterdata.pricing.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicePriceRepository extends JpaRepository<ServicePrice, Long> {

    Optional<ServicePrice> findByUid(String uid);

    /**
     * The exact price cell — (payer, service, currency). {@code planUid} null
     * addresses the cash / public price. This is the full unique key.
     */
    @Query("""
            SELECT p FROM ServicePrice p
            WHERE ((:planUid IS NULL AND p.planUid IS NULL) OR p.planUid = :planUid)
              AND p.kind = :kind
              AND p.serviceUid = :serviceUid
              AND p.currency = :currency
            """)
    Optional<ServicePrice> findCell(@Param("planUid") String planUid,
                                    @Param("kind") ServiceKind kind,
                                    @Param("serviceUid") String serviceUid,
                                    @Param("currency") String currency);

    /**
     * All currency variants for a (payer, service) cell, ordered by currency —
     * used as the last-resort fallback when no row matches the target currency.
     */
    @Query("""
            SELECT p FROM ServicePrice p
            WHERE ((:planUid IS NULL AND p.planUid IS NULL) OR p.planUid = :planUid)
              AND p.kind = :kind
              AND p.serviceUid = :serviceUid
            ORDER BY p.currency ASC
            """)
    List<ServicePrice> findCellAnyCurrency(@Param("planUid") String planUid,
                                           @Param("kind") ServiceKind kind,
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
