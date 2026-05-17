package com.otapp.hmis.engine.iam.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("""
            SELECT a FROM LoginAttempt a
            WHERE (:username IS NULL OR :username = '' OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%')))
              AND (:outcome IS NULL OR a.outcome = :outcome)
            """)
    Page<LoginAttempt> search(@Param("username") String username,
                              @Param("outcome") LoginAttempt.Outcome outcome,
                              Pageable pageable);
}
