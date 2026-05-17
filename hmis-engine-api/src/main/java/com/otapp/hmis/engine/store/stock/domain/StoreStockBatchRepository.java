package com.otapp.hmis.engine.store.stock.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreStockBatchRepository extends JpaRepository<StoreStockBatch, Long> {

    Optional<StoreStockBatch> findByUid(String uid);

    Optional<StoreStockBatch> findByStoreUidAndMedicineUidAndBatchNo(String storeUid,
                                                                    String medicineUid,
                                                                    String batchNo);

    /**
     * FEFO order: earliest-expiring first, NULL expiry last, then by
     * received_at for deterministic tie-break. Pessimistic-write lock so
     * concurrent issues don't double-spend the same batch.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM StoreStockBatch b
            WHERE b.storeUid = :storeUid
              AND b.medicineUid = :medicineUid
              AND b.quantity > 0
            ORDER BY CASE WHEN b.expiresAt IS NULL THEN 1 ELSE 0 END,
                     b.expiresAt ASC,
                     b.receivedAt ASC
            """)
    List<StoreStockBatch> lockFefoForIssue(@Param("storeUid") String storeUid,
                                           @Param("medicineUid") String medicineUid);

    @Query("""
            SELECT b FROM StoreStockBatch b
            WHERE b.storeUid = :storeUid
            ORDER BY b.medicineUid ASC, b.expiresAt ASC NULLS LAST, b.receivedAt ASC
            """)
    List<StoreStockBatch> findAllByStore(@Param("storeUid") String storeUid);
}
