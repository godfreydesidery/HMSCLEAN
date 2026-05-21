package com.otapp.hmis.engine.iam.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, Long> {

    Optional<ProviderProfile> findByUserUid(String userUid);

    /** Batch enrichment for staff listings (avoids N+1). */
    List<ProviderProfile> findAllByUserUidIn(Collection<String> userUids);
}
