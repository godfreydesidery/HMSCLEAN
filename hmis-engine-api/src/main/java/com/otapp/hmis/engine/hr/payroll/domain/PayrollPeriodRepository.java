package com.otapp.hmis.engine.hr.payroll.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    Optional<PayrollPeriod> findByUid(String uid);

    boolean existsByCode(String code);

    @Query("""
            SELECT p FROM PayrollPeriod p
            WHERE (:status IS NULL OR p.status = :status)
            ORDER BY p.startDate DESC, p.createdAt DESC
            """)
    Page<PayrollPeriod> search(@Param("status") PayrollPeriodStatus status, Pageable pageable);
}
