package com.otapp.hmis.engine.hr.payroll.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long> {

    Optional<PayrollItem> findByUid(String uid);

    Optional<PayrollItem> findByPeriodUidAndEmployeeUid(String periodUid, String employeeUid);

    List<PayrollItem> findAllByPeriodUidOrderByCreatedAtAsc(String periodUid);

    @Query("""
            SELECT COALESCE(SUM(i.netPay), 0) FROM PayrollItem i
            WHERE i.periodUid = :periodUid
            """)
    BigDecimal sumNetForPeriod(@Param("periodUid") String periodUid);

    /** Employee uids already on the period — used by import to skip newcomers (no N+1). */
    @Query("SELECT i.employeeUid FROM PayrollItem i WHERE i.periodUid = :periodUid")
    List<String> findEmployeeUidsByPeriodUid(@Param("periodUid") String periodUid);
}
