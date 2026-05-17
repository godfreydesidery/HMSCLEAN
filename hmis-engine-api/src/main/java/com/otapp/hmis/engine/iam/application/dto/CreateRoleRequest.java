package com.otapp.hmis.engine.iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateRoleRequest(
        @NotBlank @Size(min = 2, max = 64) String name,
        @Size(max = 255) String description,
        Set<String> privileges) {
}
