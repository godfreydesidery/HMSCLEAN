package com.otapp.hmis.engine.masterdata.store.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.iam.application.StaffDirectoryService;
import com.otapp.hmis.engine.iam.application.dto.StaffOption;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.StoreDto;
import com.otapp.hmis.engine.masterdata.store.application.StoreStaffDtos.StoreStaffDto;
import com.otapp.hmis.engine.masterdata.store.domain.Store;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import com.otapp.hmis.engine.masterdata.store.domain.StoreStaff;
import com.otapp.hmis.engine.masterdata.store.domain.StoreStaffRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the store keeper ⇄ store affiliation. Assignment validates the user
 * against {@code iam} (must exist, be enabled and hold the {@code STORE_PERSON}
 * role); {@link #isAssigned(String, String)} is the read-only gate consumed by
 * the {@code transfer} module when goods are issued from a store.
 */
@Service
@RequiredArgsConstructor
public class StoreStaffService {

    static final String STORE_PERSON_ROLE = "STORE_PERSON";

    private final StoreStaffRepository repository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StaffDirectoryService staffDirectory;

    /** Idempotent: re-assigning an existing (possibly removed) keeper reactivates the row. */
    @Transactional
    public StoreStaffDto assign(String storeUid, String userUid) {
        Store store = storeRepository.findByUid(storeUid)
                .orElseThrow(() -> new NotFoundException("Store not found: " + storeUid));
        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new NotFoundException("User not found: " + userUid));
        if (!user.isEnabled()) {
            throw new BusinessRuleException("User account is disabled: " + user.getUsername());
        }
        if (!staffDirectory.isUserInRole(user.getUsername(), STORE_PERSON_ROLE)) {
            throw new BusinessRuleException("User " + user.getUsername() + " is not a store keeper");
        }

        StoreStaff row = repository.findByStoreUidAndUserUid(store.getUid(), user.getUid())
                .map(existing -> {
                    existing.activate();
                    return existing;
                })
                .orElseGet(() -> repository.save(
                        new StoreStaff(store.getUid(), user.getUid(), user.getUsername())));
        return toDto(row);
    }

    @Transactional
    public void remove(String storeUid, String userUid) {
        StoreStaff row = repository.findByStoreUidAndUserUid(storeUid, userUid)
                .orElseThrow(() -> new NotFoundException(
                        "Store keeper " + userUid + " is not assigned to store " + storeUid));
        row.deactivate();
    }

    @Transactional(readOnly = true)
    public List<StoreStaffDto> listForStore(String storeUid) {
        return repository.findByStoreUidAndActiveTrueOrderByUsername(storeUid).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreStaffDto> listForUser(String userUid) {
        return repository.findByUserUidAndActiveTrueOrderByStoreUid(userUid).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * The active stores the currently-authenticated user is affiliated with —
     * backs the storekeeper "my stores" picker (legacy
     * {@code load_stores_by_store_person}). Returned in the same shape as the
     * store list endpoint. An unaffiliated user (e.g. ROOT) yields an empty list.
     */
    @Transactional(readOnly = true)
    public List<StoreDto> listMyStores() {
        return repository.findActiveStoresForUsername(currentUsername()).stream()
                .map(StoreService::toDto)
                .toList();
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user required");
        }
        return auth.getName();
    }

    /** Issue / transfer gate: is this keeper currently affiliated with the source store? */
    @Transactional(readOnly = true)
    public boolean isAssigned(String storeUid, String username) {
        return repository.existsByStoreUidAndUsernameAndActiveTrue(storeUid, username);
    }

    private StoreStaffDto toDto(StoreStaff row) {
        StaffOption staff = staffDirectory.findByUsername(row.getUsername()).orElse(null);
        return new StoreStaffDto(
                row.getUid(),
                row.getStoreUid(),
                row.getUserUid(),
                row.getUsername(),
                staff == null ? row.getUsername() : staff.fullName(),
                row.isActive(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
