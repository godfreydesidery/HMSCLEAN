package com.otapp.hmis.engine.store.stock.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
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
}
