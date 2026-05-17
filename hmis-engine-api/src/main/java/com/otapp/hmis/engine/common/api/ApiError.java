package com.otapp.hmis.engine.common.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> errors) {

    public record FieldError(String field, String message) {}

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, List.of());
    }

    public static ApiError of(int status, String code, String message, String path, List<FieldError> errors) {
        return new ApiError(Instant.now(), status, code, message, path, errors);
    }
}
