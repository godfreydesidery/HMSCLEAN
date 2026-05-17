package com.otapp.hmis.engine.masterdata.labtest.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabTestTypeRepository extends JpaRepository<LabTestType, Long> {

    Optional<LabTestType> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT l FROM LabTestType l
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(l.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR l.active = :active)
            """)
    Page<LabTestType> search(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
