package com.otapp.hmis.engine.common.api;

import com.otapp.hmis.engine.common.error.DomainException;
import com.otapp.hmis.engine.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex, HttpServletRequest req) {
        log.debug("Domain exception: {}", ex.getMessage());
        ErrorCode code = ex.code();
        ApiError body = ApiError.of(code.status().value(), code.name(), ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(code.status()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiError body = ApiError.of(
                ErrorCode.VALIDATION_FAILED.status().value(),
                ErrorCode.VALIDATION_FAILED.name(),
                "Validation failed",
                req.getRequestURI(),
                errors);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status()).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(
                ErrorCode.UNAUTHENTICATED.status().value(),
                ErrorCode.UNAUTHENTICATED.name(),
                ex.getMessage(),
                req.getRequestURI());
        return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.status()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(
                ErrorCode.FORBIDDEN.status().value(),
                ErrorCode.FORBIDDEN.name(),
                ex.getMessage(),
                req.getRequestURI());
        return ResponseEntity.status(ErrorCode.FORBIDDEN.status()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        ApiError body = ApiError.of(
                ErrorCode.INTERNAL.status().value(),
                ErrorCode.INTERNAL.name(),
                "Unexpected error",
                req.getRequestURI());
        return ResponseEntity.status(ErrorCode.INTERNAL.status()).body(body);
    }
}
