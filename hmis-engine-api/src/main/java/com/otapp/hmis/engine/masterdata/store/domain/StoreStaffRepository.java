package com.otapp.hmis.engine.masterdata.store.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {

    Optional<StoreStaff> findByStoreUidAndUserUid(String storeUid, String userUid);

    /** Backs the issue / transfer gate: is this keeper currently affiliated with the store? */
    boolean existsByStoreUidAndUsernameAndActiveTrue(String storeUid, String username);

    List<StoreStaff> findByStoreUidAndActiveTrueOrderByUsername(String storeUid);

    List<StoreStaff> findByUserUidAndActiveTrueOrderByStoreUid(String userUid);

    /**
     * The active stores a keeper is currently affiliated with — backs the
     * "my stores" picker (legacy {@code load_stores_by_store_person}). Joins the
     * affiliation rows to {@code Store} by uid, keeping only active stores, ordered
     * by name. Coupling to {@code Store} is intra-module, so a join query is clean.
     */
    @Query("""
            SELECT s FROM Store s, StoreStaff ss
            WHERE ss.username = :username
              AND ss.active = true
              AND s.uid = ss.storeUid
              AND s.active = true
            ORDER BY s.name
            """)
    List<Store> findActiveStoresForUsername(@Param("username") String username);
}
