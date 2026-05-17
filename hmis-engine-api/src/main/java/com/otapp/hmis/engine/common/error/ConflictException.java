package com.otapp.hmis.engine.common.error;

public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
