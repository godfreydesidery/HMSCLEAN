package com.otapp.hmis.engine.common.error;

/**
 * Base class for business-rule violations that should surface to the API
 * as a 4xx response. Subclasses define the precise status mapping via
 * {@link ErrorCode}.
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode code;

    protected DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    protected DomainException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
