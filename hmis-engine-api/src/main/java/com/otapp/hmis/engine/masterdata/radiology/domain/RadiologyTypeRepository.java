package com.otapp.hmis.engine.masterdata.radiology.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RadiologyTypeRepository extends JpaRepository<RadiologyType, Long> {

    Optional<RadiologyType> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT r FROM RadiologyType r
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active   IS NULL OR r.active   = :active)
              AND (:modality IS NULL OR r.modality = :modality)
            """)
    Page<RadiologyType> search(@Param("search") String search,
                               @Param("active") Boolean active,
                               @Param("modality") RadiologyModality modality,
                               Pageable pageable);
}
