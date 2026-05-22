package com.otapp.hmis.engine.encounter.consumable.domain;

/**
 * Where a ward consumable was pulled from when it was issued to a patient.
 * The source uid (store / pharmacy) is captured on the
 * {@link ConsumableIssue} row alongside this discriminator.
 */
public enum ConsumableSourceKind {
    STORE,
    PHARMACY
}
