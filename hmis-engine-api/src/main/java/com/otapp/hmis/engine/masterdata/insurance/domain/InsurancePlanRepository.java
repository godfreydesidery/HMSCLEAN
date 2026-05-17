package com.otapp.hmis.engine.masterdata.insurance.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsurancePlanRepository extends JpaRepository<InsurancePlan, Long> {

    Optional<InsurancePlan> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT p FROM InsurancePlan p
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active      IS NULL OR p.active      = :active)
              AND (:providerUid IS NULL OR p.providerUid = :providerUid)
            """)
    Page<InsurancePlan> search(@Param("search") String search,
                               @Param("active") Boolean active,
                               @Param("providerUid") String providerUid,
                               Pageable pageable);
}
