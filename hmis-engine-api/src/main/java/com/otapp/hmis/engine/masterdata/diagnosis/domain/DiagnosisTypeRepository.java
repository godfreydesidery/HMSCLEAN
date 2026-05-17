package com.otapp.hmis.engine.masterdata.diagnosis.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiagnosisTypeRepository extends JpaRepository<DiagnosisType, Long> {

    Optional<DiagnosisType> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT d FROM DiagnosisType d
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR d.active = :active)
            """)
    Page<DiagnosisType> search(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
