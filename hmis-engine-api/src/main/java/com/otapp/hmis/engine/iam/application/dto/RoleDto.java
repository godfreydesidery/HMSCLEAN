package com.otapp.hmis.engine.iam.application.dto;

import java.util.List;

public record RoleDto(
        String uid,
        String name,
        String description,
        List<String> privileges) {
}
