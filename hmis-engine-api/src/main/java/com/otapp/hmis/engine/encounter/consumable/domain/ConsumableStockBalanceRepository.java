package com.otapp.hmis.engine.encounter.consumable.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsumableStockBalanceRepository extends JpaRepository<ConsumableStockBalance, Long> {

    Optional<ConsumableStockBalance> findBySourceKindAndSourceUidAndConsumableUid(
            ConsumableSourceKind sourceKind, String sourceUid, String consumableUid);

    /** Pessimistic-write lock so concurrent issues / decrements don't double-spend. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM ConsumableStockBalance b
            WHERE b.sourceKind     = :sourceKind
              AND b.sourceUid      = :sourceUid
              AND b.consumableUid  = :consumableUid
            """)
    Optional<ConsumableStockBalance> lockForUpdate(@Param("sourceKind") ConsumableSourceKind sourceKind,
                                                   @Param("sourceUid") String sourceUid,
                                                   @Param("consumableUid") String consumableUid);

    @Query("""
            SELECT b FROM ConsumableStockBalance b
            WHERE b.sourceKind = :sourceKind
              AND b.sourceUid  = :sourceUid
            ORDER BY b.consumableUid ASC
            """)
    List<ConsumableStockBalance> findAllBySource(@Param("sourceKind") ConsumableSourceKind sourceKind,
                                                 @Param("sourceUid") String sourceUid);
}
