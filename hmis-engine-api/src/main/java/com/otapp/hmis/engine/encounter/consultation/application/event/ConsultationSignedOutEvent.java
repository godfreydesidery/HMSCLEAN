package com.otapp.hmis.engine.encounter.consultation.application.event;

/**
 * Published from {@code ConsultationService.complete} (legacy sign-out /
 * free_consultation) once the consultation closes as COMPLETED and its
 * downstream unsettled orders / prescriptions have been cancelled
 * encounter-side. Billing listens after-commit and voids the now-cancelled,
 * still-unpaid downstream invoice lines (recomputing the invoice subtotal);
 * PAID lines are left intact (legacy "only UNPAID downstream is cancelled").
 *
 * <p>Direction is encounter → billing via events; the encounter module never
 * imports billing.
 */
public record ConsultationSignedOutEvent(String consultationUid, String patientUid) {}
