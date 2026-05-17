package com.otapp.hmis.engine.masterdata.store.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT s FROM Store s
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR s.active = :active)
            """)
    Page<Store> search(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
