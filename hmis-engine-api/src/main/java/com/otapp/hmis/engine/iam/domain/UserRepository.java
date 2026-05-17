package com.otapp.hmis.engine.iam.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUid(String uid);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findAllByEnabled(boolean enabled, Pageable pageable);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            WHERE r.name = :roleName
              AND u.enabled = TRUE
            ORDER BY u.firstName, u.lastName
            """)
    List<User> findEnabledByRoleName(@Param("roleName") String roleName);

    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(u.username)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:enabled IS NULL OR u.enabled = :enabled)
            """)
    Page<User> search(@Param("search") String search,
                      @Param("enabled") Boolean enabled,
                      Pageable pageable);
}
