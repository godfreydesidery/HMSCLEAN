package com.otapp.hmis.engine.encounter.consumable.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumableIssueRepository extends JpaRepository<ConsumableIssue, Long> {

    Optional<ConsumableIssue> findByUid(String uid);

    List<ConsumableIssue> findAllByAdmissionUidOrderByIssuedAtAsc(String admissionUid);
}
