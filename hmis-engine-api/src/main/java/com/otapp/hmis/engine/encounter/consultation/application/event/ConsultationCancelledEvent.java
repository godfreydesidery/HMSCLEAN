package com.otapp.hmis.engine.encounter.consultation.application.event;

/**
 * Published from {@code ConsultationService.cancel} once a PENDING (BOOKED)
 * consultation is cancelled. Billing listens after-commit and runs the legacy
 * cancel cascade against the consultation-fee invoice: refund any received
 * payment and raise a credit note, then cancel the (now-unpaid) invoice.
 *
 * <p>Direction is encounter → billing via events (billing depends on encounter,
 * never the reverse). The encounter module never imports billing; it only
 * flips the consultation's own status and publishes this signal.
 */
public record ConsultationCancelledEvent(String consultationUid, String patientUid) {}
