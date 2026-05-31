package com.otapp.hmis.engine.billing.claim.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByUid(String uid);

    @Query("""
            SELECT c FROM Claim c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:payerPlanUid IS NULL OR c.payerPlanUid = :payerPlanUid)
              AND (:providerUid IS NULL OR c.providerUid = :providerUid)
              AND (:membershipNo IS NULL OR c.membershipNo = :membershipNo)
            ORDER BY c.createdAt DESC
            """)
    Page<Claim> search(@Param("status") ClaimStatus status,
                       @Param("payerPlanUid") String payerPlanUid,
                       @Param("providerUid") String providerUid,
                       @Param("membershipNo") String membershipNo,
                       Pageable pageable);
}
