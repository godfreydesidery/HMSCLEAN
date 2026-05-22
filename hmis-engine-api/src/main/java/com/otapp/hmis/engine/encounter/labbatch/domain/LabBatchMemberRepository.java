package com.otapp.hmis.engine.encounter.labbatch.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabBatchMemberRepository extends JpaRepository<LabBatchMember, Long> {

    List<LabBatchMember> findAllByBatchUidOrderByCreatedAtAsc(String batchUid);

    Optional<LabBatchMember> findByOrderUid(String orderUid);

    /** Members for any of the given orders — used to filter already-batched candidates. */
    List<LabBatchMember> findAllByOrderUidIn(Collection<String> orderUids);

    void deleteByBatchUidAndOrderUid(String batchUid, String orderUid);

    long countByBatchUid(String batchUid);
}
