package com.otapp.hmis.engine.masterdata.administrationroute.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdministrationRouteRepository extends JpaRepository<AdministrationRoute, Long> {

    Optional<AdministrationRoute> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT r FROM AdministrationRoute r
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR r.active = :active)
            """)
    Page<AdministrationRoute> search(@Param("search") String search,
                                     @Param("active") Boolean active,
                                     Pageable pageable);
}
