package com.otapp.hmis.engine.iam.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RoleDto(
        String uid,
        String name,
        String description,
        List<String> privileges,
        /**
         * Additive: {@code true} for protected/system roles whose privileges
         * cannot be modified. Serialised as JSON field {@code protected} so the
         * UI can render the lock state; the Java component is named
         * {@code protectedRole} because {@code protected} is a reserved word.
         */
        @JsonProperty("protected") boolean protectedRole) {
}
