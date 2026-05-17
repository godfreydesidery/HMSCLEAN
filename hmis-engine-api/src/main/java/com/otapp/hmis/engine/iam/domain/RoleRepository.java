package com.otapp.hmis.engine.iam.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByUid(String uid);

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}
