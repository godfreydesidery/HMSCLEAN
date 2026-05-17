package com.otapp.hmis.engine.masterdata.insurance.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsuranceProviderRepository extends JpaRepository<InsuranceProvider, Long> {

    Optional<InsuranceProvider> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT i FROM InsuranceProvider i
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(i.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR i.active = :active)
            """)
    Page<InsuranceProvider> search(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
