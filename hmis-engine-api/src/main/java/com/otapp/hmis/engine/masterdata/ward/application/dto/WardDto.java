package com.otapp.hmis.engine.masterdata.ward.application.dto;

import com.otapp.hmis.engine.masterdata.ward.domain.WardCategory;
import java.time.Instant;

public record WardDto(
        String uid,
        String code,
        String name,
        WardCategory category,
        int capacity,
        String location,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
