package com.otapp.hmis.engine.encounter.labbatch.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabBatchRepository extends JpaRepository<LabBatch, Long> {

    Optional<LabBatch> findByUid(String uid);

    @Query("""
            SELECT b FROM LabBatch b
            WHERE (:status         IS NULL OR b.status         = :status)
              AND (:labTestTypeUid IS NULL OR b.labTestTypeUid = :labTestTypeUid)
            ORDER BY b.openedAt DESC
            """)
    Page<LabBatch> search(@Param("status") LabBatchStatus status,
                          @Param("labTestTypeUid") String labTestTypeUid,
                          Pageable pageable);
}
