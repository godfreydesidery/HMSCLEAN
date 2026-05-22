package com.otapp.hmis.engine.hr.payroll.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollComponentBandRepository extends JpaRepository<PayrollComponentBand, Long> {

    List<PayrollComponentBand> findAllByComponentUidOrderBySortOrderAsc(String componentUid);

    /** Bands for any of the given components — bulk load for the compute pass. */
    List<PayrollComponentBand> findAllByComponentUidInOrderBySortOrderAsc(Collection<String> componentUids);

    void deleteByComponentUid(String componentUid);
}
