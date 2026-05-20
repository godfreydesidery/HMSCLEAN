package com.otapp.hmis.engine.hr.payroll.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, Long> {

    Optional<PayrollComponent> findByUid(String uid);

    boolean existsByCode(String code);

    /** Active components in apply order — drives the auto-prefill compute. */
    List<PayrollComponent> findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc();

    @Query("""
            SELECT c FROM PayrollComponent c
            WHERE (:active IS NULL OR c.active = :active)
              AND (:type   IS NULL OR c.type = :type)
            ORDER BY c.sortOrder ASC, c.createdAt ASC
            """)
    Page<PayrollComponent> search(@Param("active") Boolean active,
                                  @Param("type") PayrollComponentType type,
                                  Pageable pageable);
}
