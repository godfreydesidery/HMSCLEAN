package com.otapp.hmis.engine.encounter.operative.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperativeRecordRepository extends JpaRepository<OperativeRecord, Long> {

    Optional<OperativeRecord> findByUid(String uid);

    Optional<OperativeRecord> findByOrderUid(String orderUid);
}
