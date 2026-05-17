package com.otapp.hmis.engine.iam.application.dto;

import java.util.List;

public record LoginResponse(
        TokenPair tokens,
        UserSummary user,
        List<String> roles,
        List<String> privileges,
        boolean passwordMustChange) {
}
