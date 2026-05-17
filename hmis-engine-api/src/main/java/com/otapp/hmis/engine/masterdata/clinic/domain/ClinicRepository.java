package com.otapp.hmis.engine.masterdata.clinic.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    Optional<Clinic> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            SELECT c FROM Clinic c
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR c.active = :active)
              AND (:type   IS NULL OR c.type   = :type)
            """)
    Page<Clinic> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("type") ClinicType type,
            Pageable pageable);
}
