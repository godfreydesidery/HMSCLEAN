package com.otapp.hmis.engine.encounter.labbatch.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabBatchMemberRepository extends JpaRepository<LabBatchMember, Long> {

    List<LabBatchMember> findAllByBatchUidOrderByCreatedAtAsc(String batchUid);

    Optional<LabBatchMember> findByOrderUid(String orderUid);

    void deleteByBatchUidAndOrderUid(String batchUid, String orderUid);

    long countByBatchUid(String batchUid);
}
