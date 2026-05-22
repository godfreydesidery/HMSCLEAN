package com.otapp.hmis.engine.masterdata.currency.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByUid(String uid);

    Optional<Currency> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Currency> findByDefaultCurrencyTrue();

    boolean existsByCodeAndActiveTrue(String code);

    /**
     * Clears the default flag on every row. Called before marking a new default
     * so the {@code uk_md_currency_default} partial unique index is never
     * violated (at most one {@code is_default = TRUE} row).
     */
    @Modifying
    @Query("UPDATE Currency c SET c.defaultCurrency = false WHERE c.defaultCurrency = true")
    void clearDefaultFlag();

    @Query("""
            SELECT c FROM Currency c
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR c.active = :active)
            """)
    Page<Currency> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}
