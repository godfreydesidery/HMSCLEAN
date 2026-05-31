package com.otapp.hmis.engine.procurement.pricelist.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierItemPriceRepository extends JpaRepository<SupplierItemPrice, Long> {

    Optional<SupplierItemPrice> findByUid(String uid);

    List<SupplierItemPrice> findBySupplierUidOrderByValidFromDesc(String supplierUid);

    List<SupplierItemPrice> findByMedicineUidOrderByValidFromDesc(String medicineUid);

    /**
     * Quotes for a (supplier, medicine) pair, newest first. Used when
     * editing a single supplier's history for one medicine.
     */
    List<SupplierItemPrice> findBySupplierUidAndMedicineUidOrderByValidFromDesc(String supplierUid,
                                                                                String medicineUid);

    /**
     * All currently-valid (active + today within window) quotes for a
     * medicine, ordered cheapest first — that's the "comparison shop"
     * view used to pick the best supplier for an LPO line.
     */
    @Query("""
            SELECT p FROM SupplierItemPrice p
            WHERE p.medicineUid = :medicineUid
              AND p.active = TRUE
              AND p.validFrom <= :today
              AND (p.validTo IS NULL OR p.validTo >= :today)
            ORDER BY p.unitPrice ASC, p.validFrom DESC
            """)
    List<SupplierItemPrice> findActiveForMedicine(@Param("medicineUid") String medicineUid,
                                                  @Param("today") LocalDate today);

    /**
     * The supplier's CURRENT contracted quote for one medicine — active and
     * today within the window — newest {@code validFrom} first. This is the
     * legacy {@code findBySupplierAndItem} gate (a supplier must quote an item
     * before it can be ordered from them); the date window simply rolls the
     * quote forward. The caller takes the first (newest {@code validFrom})
     * element as the contracted price.
     */
    @Query("""
            SELECT p FROM SupplierItemPrice p
            WHERE p.supplierUid = :supplierUid
              AND p.medicineUid = :medicineUid
              AND p.active = TRUE
              AND p.validFrom <= :today
              AND (p.validTo IS NULL OR p.validTo >= :today)
            ORDER BY p.validFrom DESC
            """)
    List<SupplierItemPrice> findCurrentForSupplierAndMedicine(@Param("supplierUid") String supplierUid,
                                                              @Param("medicineUid") String medicineUid,
                                                              @Param("today") LocalDate today);
}
