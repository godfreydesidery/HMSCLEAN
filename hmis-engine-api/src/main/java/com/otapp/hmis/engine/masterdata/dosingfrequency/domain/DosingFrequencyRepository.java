package com.otapp.hmis.engine.masterdata.dosingfrequency.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DosingFrequencyRepository extends JpaRepository<DosingFrequency, Long> {

    Optional<DosingFrequency> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT f FROM DosingFrequency f
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(f.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR f.active = :active)
            """)
    Page<DosingFrequency> search(@Param("search") String search,
                                 @Param("active") Boolean active,
                                 Pageable pageable);
}
