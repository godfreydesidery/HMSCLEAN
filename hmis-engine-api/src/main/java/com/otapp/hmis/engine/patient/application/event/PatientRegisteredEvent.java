package com.otapp.hmis.engine.patient.application.event;

/**
 * Published by {@code PatientService.register} after a new patient is
 * persisted. Listeners in downstream modules (billing) use this to seed the
 * registration-fee invoice without forcing the patient module to depend on
 * billing.
 */
public record PatientRegisteredEvent(String patientUid) {}
