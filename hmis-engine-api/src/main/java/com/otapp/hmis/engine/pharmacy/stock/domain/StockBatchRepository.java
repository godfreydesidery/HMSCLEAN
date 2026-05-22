package com.otapp.hmis.engine.pharmacy.stock.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    Optional<StockBatch> findByUid(String uid);

    Optional<StockBatch> findByPharmacyUidAndMedicineUidAndBatchNo(String pharmacyUid,
                                                                  String medicineUid,
                                                                  String batchNo);

    /**
     * FEFO order: earliest-expiring first, NULL expiry last, then by
     * received_at for deterministic tie-break. Pessimistic-write lock so
     * concurrent dispenses don't double-spend the same batch.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM StockBatch b
            WHERE b.pharmacyUid = :pharmacyUid
              AND b.medicineUid = :medicineUid
              AND b.quantity > 0
            ORDER BY CASE WHEN b.expiresAt IS NULL THEN 1 ELSE 0 END,
                     b.expiresAt ASC,
                     b.receivedAt ASC
            """)
    List<StockBatch> lockFefoForDispense(@Param("pharmacyUid") String pharmacyUid,
                                         @Param("medicineUid") String medicineUid);

    @Query("""
            SELECT b FROM StockBatch b
            WHERE b.pharmacyUid = :pharmacyUid
            ORDER BY b.medicineUid ASC, b.expiresAt ASC NULLS LAST, b.receivedAt ASC
            """)
    List<StockBatch> findAllByPharmacy(@Param("pharmacyUid") String pharmacyUid);

    /** Batches for one (pharmacy, medicine) in FEFO display order — used to roll up a balance row. */
    @Query("""
            SELECT b FROM StockBatch b
            WHERE b.pharmacyUid = :pharmacyUid AND b.medicineUid = :medicineUid
            ORDER BY CASE WHEN b.expiresAt IS NULL THEN 1 ELSE 0 END, b.expiresAt ASC, b.receivedAt ASC
            """)
    List<StockBatch> findAllByPharmacyUidAndMedicineUid(@Param("pharmacyUid") String pharmacyUid,
                                                        @Param("medicineUid") String medicineUid);

    /** Non-empty batches expiring on or before {@code threshold} — drives the expiry report. */
    @Query("""
            SELECT b FROM StockBatch b
            WHERE b.expiresAt IS NOT NULL
              AND b.expiresAt <= :threshold
              AND b.quantity > 0
            ORDER BY b.expiresAt ASC
            """)
    List<StockBatch> findExpiringBy(@Param("threshold") java.time.LocalDate threshold);
}
