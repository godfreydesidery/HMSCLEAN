package com.otapp.hmis.engine.store.stock.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreStockBalanceRepository extends JpaRepository<StoreStockBalance, Long> {

    Optional<StoreStockBalance> findByStoreUidAndMedicineUid(String storeUid, String medicineUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StoreStockBalance b WHERE b.storeUid = :storeUid AND b.medicineUid = :medicineUid")
    Optional<StoreStockBalance> lockByStoreUidAndMedicineUid(@Param("storeUid") String storeUid,
                                                             @Param("medicineUid") String medicineUid);

    List<StoreStockBalance> findAllByStoreUid(String storeUid);

    /** Balances at or below {@code threshold} across all stores — used by the stock-out report. */
    List<StoreStockBalance> findByQuantityLessThanEqual(int threshold);

    /**
     * Server-side store stock search: optional medicine name/code text,
     * low-stock-only, and expiring-only (any non-empty batch on/under the cutoff).
     */
    @Query("""
            SELECT b FROM StoreStockBalance b
            WHERE b.storeUid = :storeUid
              AND (:lowStock = FALSE OR b.quantity <= :lowStockThreshold)
              AND (:search IS NULL OR :search = '' OR EXISTS (
                    SELECT 1 FROM Medicine m
                    WHERE m.uid = b.medicineUid
                      AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%')))))
              AND (:expiringOnly = FALSE OR EXISTS (
                    SELECT 1 FROM StoreStockBatch sb
                    WHERE sb.storeUid = b.storeUid
                      AND sb.medicineUid = b.medicineUid
                      AND sb.quantity > 0
                      AND sb.expiresAt IS NOT NULL
                      AND sb.expiresAt <= :expiryCutoff))
            """)
    Page<StoreStockBalance> searchBalances(@Param("storeUid") String storeUid,
                                           @Param("search") String search,
                                           @Param("lowStock") boolean lowStock,
                                           @Param("lowStockThreshold") int lowStockThreshold,
                                           @Param("expiringOnly") boolean expiringOnly,
                                           @Param("expiryCutoff") java.time.LocalDate expiryCutoff,
                                           Pageable pageable);
}
