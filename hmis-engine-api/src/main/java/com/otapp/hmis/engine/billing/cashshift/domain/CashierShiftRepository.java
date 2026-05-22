package com.otapp.hmis.engine.billing.cashshift.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashierShiftRepository extends JpaRepository<CashierShift, Long> {

    Optional<CashierShift> findByUid(String uid);

    Optional<CashierShift> findFirstByCashierUsernameAndStatus(String cashierUsername,
                                                               CashierShiftStatus status);

    @Query("""
            SELECT s FROM CashierShift s
            WHERE (:username IS NULL OR :username = '' OR LOWER(s.cashierUsername) = LOWER(:username))
              AND (:status   IS NULL OR s.status = :status)
            ORDER BY s.openedAt DESC
            """)
    Page<CashierShift> search(@Param("username") String username,
                              @Param("status") CashierShiftStatus status,
                              Pageable pageable);
}
