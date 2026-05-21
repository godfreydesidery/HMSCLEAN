package com.otapp.hmis.engine.masterdata.store.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class StoreStaffDtos {

    private StoreStaffDtos() {
    }

    public record AssignStoreStaffRequest(
            @NotBlank @Size(max = 26) String userUid) {
    }

    public record StoreStaffDto(
            String uid,
            String storeUid,
            String userUid,
            String username,
            String fullName,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }
}
