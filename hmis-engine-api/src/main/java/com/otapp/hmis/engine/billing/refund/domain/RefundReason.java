package com.otapp.hmis.engine.billing.refund.domain;

/**
 * Why money is being returned to the patient (PROCESS.md §11, §16).
 */
public enum RefundReason {
    OVERPAYMENT,
    SERVICE_NOT_RENDERED,
    DOUBLE_PAYMENT,
    CANCELLATION,
    OTHER
}
