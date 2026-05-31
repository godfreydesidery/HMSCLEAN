package com.otapp.hmis.engine.encounter.admission.application.event;

/**
 * Published from {@code AdmissionService.cancel} once an admission is cancelled.
 * Billing listens after-commit and voids the (now-abandoned) ward-bed invoice so a
 * cancelled deposit-pending admission does not leave a live unpaid receivable on the
 * books — the admission analogue of {@code ConsultationCancelledEvent}.
 *
 * <p>Direction is encounter → billing via events (billing depends on encounter,
 * never the reverse); the encounter module never imports billing.
 */
public record AdmissionCancelledEvent(String admissionUid) {}
