package com.otapp.hmis.engine.masterdata.medicine.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT m FROM Medicine m
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR m.active = :active)
              AND (:form   IS NULL OR m.form   = :form)
            """)
    Page<Medicine> search(@Param("search") String search,
                          @Param("active") Boolean active,
                          @Param("form") MedicineForm form,
                          Pageable pageable);
}
