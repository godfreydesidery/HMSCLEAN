package com.otapp.hmis.engine.pharmacy.stock.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Optional<StockMovement> findByUid(String uid);

    @Query("""
            SELECT m FROM StockMovement m
            WHERE (:pharmacyUid IS NULL OR m.pharmacyUid = :pharmacyUid)
              AND (:medicineUid IS NULL OR m.medicineUid = :medicineUid)
              AND (:kind        IS NULL OR m.kind        = :kind)
            """)
    Page<StockMovement> search(@Param("pharmacyUid") String pharmacyUid,
                               @Param("medicineUid") String medicineUid,
                               @Param("kind") StockMovementKind kind,
                               Pageable pageable);
}
