package com.otapp.hmis.engine.billing.invoice.domain;

/**
 * What kind of service a line represents. Matches the masterdata.pricing
 * ServiceKind enum (string values aligned) so prices can be looked up from
 * the same matrix.
 */
public enum InvoiceLineKind {
    CONSULTATION,
    LAB_TEST,
    PROCEDURE,
    RADIOLOGY,
    MEDICINE,
    WARD
}
