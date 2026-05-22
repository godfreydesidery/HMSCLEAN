package com.otapp.hmis.engine.hr.payroll.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PayrollItemLineRepository extends JpaRepository<PayrollItemLine, Long> {

    List<PayrollItemLine> findAllByItemUidOrderBySortOrderAsc(String itemUid);

    @Transactional
    void deleteByItemUid(String itemUid);
}
