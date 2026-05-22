package com.otapp.hmis.engine.masterdata.pricing.domain;

/**
 * Identifies which masterdata catalogue a {@link ServicePrice} refers to.
 * Combined with {@code serviceUid} this addresses any priced item in the
 * system without forcing one foreign-key column per service kind.
 */
public enum ServiceKind {
    CONSULTATION,   // a clinic visit (priced per Clinic)
    LAB_TEST,       // a LabTestType
    PROCEDURE,      // a ProcedureType
    RADIOLOGY,      // a RadiologyType
    MEDICINE,       // a Medicine (unit price)
    WARD,           // a Ward (per-day rate)
    REGISTRATION;   // patient registration fee (uses sentinel serviceUid below; plans may waive)

    /** Sentinel {@code serviceUid} for the singleton REGISTRATION service — there
     *  is no masterdata catalogue for "registration fee", just one global price. */
    public static final String REGISTRATION_SERVICE_UID = "DEFAULT";
}
