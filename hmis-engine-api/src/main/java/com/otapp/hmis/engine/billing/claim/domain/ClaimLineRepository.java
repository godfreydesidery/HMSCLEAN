package com.otapp.hmis.engine.billing.claim.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimLineRepository extends JpaRepository<ClaimLine, Long> {

    List<ClaimLine> findAllByClaimIdOrderByCreatedAtAsc(Long claimId);

    void deleteAllByClaimId(Long claimId);
}
