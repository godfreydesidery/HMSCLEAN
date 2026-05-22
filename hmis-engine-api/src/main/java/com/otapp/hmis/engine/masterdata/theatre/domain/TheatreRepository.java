package com.otapp.hmis.engine.masterdata.theatre.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    Optional<Theatre> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT t FROM Theatre t
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR t.active = :active)
            """)
    Page<Theatre> search(@Param("search") String search,
                         @Param("active") Boolean active,
                         Pageable pageable);
}
