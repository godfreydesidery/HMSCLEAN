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

    @Query("""
            SELECT b FROM StockBalance b
            WHERE b.pharmacyUid = :pharmacyUid
              AND (:lowStock = FALSE OR b.quantity <= :lowStockThreshold)
            """)
    Page<StockBalance> search(@Param("pharmacyUid") String pharmacyUid,
                              @Param("lowStock") boolean lowStock,
                              @Param("lowStockThreshold") int lowStockThreshold,
                              Pageable pageable);

    /** Balances at or below {@code threshold} across all pharmacies — used by the stock-out report. */
    List<StockBalance> findByQuantityLessThanEqual(int threshold);
}
