package com.otapp.hmis.engine.patient.domain;

/**
 * Categorises patients for billing / workflow rules. May expand later
 * (e.g. dependents of an employee, special programmes).
 */
public enum PatientType {
    NEW,
    RETURNING,
    REFERRAL,
    STAFF,
    DEPENDENT
}
