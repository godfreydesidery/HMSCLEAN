package com.otapp.hmis.engine.masterdata.pharmacy.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    Optional<Pharmacy> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT p FROM Pharmacy p
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<Pharmacy> search(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
