package com.otapp.hmis.engine.store.stock.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreStockMovementRepository extends JpaRepository<StoreStockMovement, Long> {

    Optional<StoreStockMovement> findByUid(String uid);

    @Query("""
            SELECT m FROM StoreStockMovement m
            WHERE (:storeUid    IS NULL OR m.storeUid    = :storeUid)
              AND (:medicineUid IS NULL OR m.medicineUid = :medicineUid)
              AND (:kind        IS NULL OR m.kind        = :kind)
            """)
    Page<StoreStockMovement> search(@Param("storeUid") String storeUid,
                                    @Param("medicineUid") String medicineUid,
                                    @Param("kind") StoreStockMovementKind kind,
                                    Pageable pageable);
}
