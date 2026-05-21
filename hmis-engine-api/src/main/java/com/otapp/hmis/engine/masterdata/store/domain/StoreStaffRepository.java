package com.otapp.hmis.engine.masterdata.store.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {

    Optional<StoreStaff> findByStoreUidAndUserUid(String storeUid, String userUid);

    /** Backs the issue / transfer gate: is this keeper currently affiliated with the store? */
    boolean existsByStoreUidAndUsernameAndActiveTrue(String storeUid, String username);

    List<StoreStaff> findByStoreUidAndActiveTrueOrderByUsername(String storeUid);

    List<StoreStaff> findByUserUidAndActiveTrueOrderByStoreUid(String userUid);
}
