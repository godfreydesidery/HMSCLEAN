package com.otapp.hmis.engine.hr.asset.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByUid(String uid);

    Optional<Asset> findByTag(String tag);

    boolean existsByTag(String tag);

    @Query("""
            SELECT a FROM Asset a
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(a.tag)      LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.name)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.serialNo) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status   IS NULL OR a.status   = :status)
              AND (:category IS NULL OR :category = '' OR LOWER(a.category) = LOWER(:category))
              AND (:location IS NULL OR :location = '' OR LOWER(a.location) = LOWER(:location))
            """)
    Page<Asset> search(@Param("search") String search,
                       @Param("status") AssetStatus status,
                       @Param("category") String category,
                       @Param("location") String location,
                       Pageable pageable);
}
