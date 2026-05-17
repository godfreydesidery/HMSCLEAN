package com.otapp.hmis.engine.common.error;

public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE, message);
    }
}
