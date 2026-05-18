package com.otapp.hmis.engine.encounter.operative.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperativeRecordAmendmentRepository
        extends JpaRepository<OperativeRecordAmendment, Long> {

    List<OperativeRecordAmendment> findByOperativeRecordUidOrderByAmendmentNoAsc(String operativeRecordUid);

    long countByOperativeRecordUid(String operativeRecordUid);
}
