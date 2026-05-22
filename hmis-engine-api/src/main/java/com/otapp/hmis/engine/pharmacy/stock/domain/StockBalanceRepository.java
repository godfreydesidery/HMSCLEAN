package com.otapp.hmis.engine.pharmacy.stock.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByPharmacyUidAndMedicineUid(String pharmacyUid, String medicineUid);

    /** Pessimistic-write lock — used by the dispense path to serialise concurrent issues. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBalance b WHERE b.pharmacyUid = :pharmacyUid AND b.medicineUid = :medicineUid")
    Optional<StockBalance> lockByPharmacyUidAndMedicineUid(@Param("pharmacyUid") String pharmacyUid,
                                                          @Param("medicineUid") String medicineUid);

    List<StockBalance> findAllByPharmacyUid(String pharmacyUid);

    /**
     * Server-side stock search for a pharmacy: optional medicine name/code text,
     * low-stock-only, and expiring-only (any non-empty batch on/under the cutoff).
     * All predicates are pushed to the DB so the UI never loads the full balance set.
     */
    @Query("""
            SELECT b FROM StockBalance b
            WHERE b.pharmacyUid = :pharmacyUid
              AND (:lowStock = FALSE OR b.quantity <= :lowStockThreshold)
              AND (:search IS NULL OR :search = '' OR EXISTS (
                    SELECT 1 FROM Medicine m
                    WHERE m.uid = b.medicineUid
                      AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%')))))
              AND (:expiringOnly = FALSE OR EXISTS (
                    SELECT 1 FROM StockBatch sb
                    WHERE sb.pharmacyUid = b.pharmacyUid
                      AND sb.medicineUid = b.medicineUid
                      AND sb.quantity > 0
                      AND sb.expiresAt IS NOT NULL
                      AND sb.expiresAt <= :expiryCutoff))
            """)
    Page<StockBalance> searchBalances(@Param("pharmacyUid") String pharmacyUid,
                                      @Param("search") String search,
                                      @Param("lowStock") boolean lowStock,
                                      @Param("lowStockThreshold") int lowStockThreshold,
                                      @Param("expiringOnly") boolean expiringOnly,
                                      @Param("expiryCutoff") java.time.LocalDate expiryCutoff,
                                      Pageable pageable);

    /** Balances at or below {@code threshold} across all pharmacies — used by the stock-out report. */
    List<StockBalance> findByQuantityLessThanEqual(int threshold);
}
