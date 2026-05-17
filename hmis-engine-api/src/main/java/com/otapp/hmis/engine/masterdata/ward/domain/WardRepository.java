package com.otapp.hmis.engine.masterdata.ward.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WardRepository extends JpaRepository<Ward, Long> {

    Optional<Ward> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT w FROM Ward w
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(w.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active   IS NULL OR w.active   = :active)
              AND (:category IS NULL OR w.category = :category)
            """)
    Page<Ward> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("category") WardCategory category,
            Pageable pageable);
}
