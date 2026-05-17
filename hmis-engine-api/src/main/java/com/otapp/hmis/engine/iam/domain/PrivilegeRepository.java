package com.otapp.hmis.engine.iam.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Optional<Privilege> findByUid(String uid);

    Optional<Privilege> findByName(String name);

    boolean existsByName(String name);
}
