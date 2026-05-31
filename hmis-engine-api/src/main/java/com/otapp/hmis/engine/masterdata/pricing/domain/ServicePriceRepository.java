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
     * The covered plan cell for a (plan, service, currency) — the charge-time
     * coverage decision (legacy {@code findBy<Catalogue>AndInsurancePlanAndCovered(item, plan, true)}).
     * Returns empty when the plan has no row, or a row that is not flagged
     * {@code covered}. {@code planUid} must be non-null (cash has no coverage).
     */
    @Query("""
            SELECT p FROM ServicePrice p
            WHERE p.planUid = :planUid
              AND p.kind = :kind
              AND p.serviceUid = :serviceUid
              AND p.currency = :currency
              AND p.covered = TRUE
            """)
    Optional<ServicePrice> findCoveredCell(@Param("planUid") String planUid,
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
            WHERE (:cashOnly = FALSE OR p.planUid IS NULL)
              AND (:planUid    IS NULL OR p.planUid    = :planUid)
              AND (:kind       IS NULL OR p.kind       = :kind)
              AND (:serviceUid IS NULL OR p.serviceUid = :serviceUid)
              AND (:currency   IS NULL OR p.currency   = :currency)
              AND (:search IS NULL OR :search = ''
                   OR EXISTS (SELECT 1 FROM Clinic c        WHERE c.uid  = p.serviceUid AND LOWER(c.name)  LIKE LOWER(CONCAT('%', :search, '%')))
                   OR EXISTS (SELECT 1 FROM LabTestType lt  WHERE lt.uid = p.serviceUid AND LOWER(lt.name) LIKE LOWER(CONCAT('%', :search, '%')))
                   OR EXISTS (SELECT 1 FROM ProcedureType pt WHERE pt.uid = p.serviceUid AND LOWER(pt.name) LIKE LOWER(CONCAT('%', :search, '%')))
                   OR EXISTS (SELECT 1 FROM RadiologyType rt WHERE rt.uid = p.serviceUid AND LOWER(rt.name) LIKE LOWER(CONCAT('%', :search, '%')))
                   OR EXISTS (SELECT 1 FROM Medicine md     WHERE md.uid = p.serviceUid AND LOWER(md.name) LIKE LOWER(CONCAT('%', :search, '%')))
                   OR EXISTS (SELECT 1 FROM Ward wd         WHERE wd.uid = p.serviceUid AND LOWER(wd.name) LIKE LOWER(CONCAT('%', :search, '%'))))
            """)
    Page<ServicePrice> search(@Param("planUid")    String planUid,
                              @Param("cashOnly")   boolean cashOnly,
                              @Param("kind")       ServiceKind kind,
                              @Param("serviceUid") String serviceUid,
                              @Param("currency")   String currency,
                              @Param("search")     String search,
                              Pageable pageable);
}
