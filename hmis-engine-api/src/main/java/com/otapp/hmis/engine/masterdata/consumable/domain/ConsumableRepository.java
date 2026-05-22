package com.otapp.hmis.engine.masterdata.consumable.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsumableRepository extends JpaRepository<Consumable, Long> {

    Optional<Consumable> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT c FROM Consumable c
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR c.active = :active)
            """)
    Page<Consumable> search(@Param("search") String search,
                            @Param("active") Boolean active,
                            Pageable pageable);
}
