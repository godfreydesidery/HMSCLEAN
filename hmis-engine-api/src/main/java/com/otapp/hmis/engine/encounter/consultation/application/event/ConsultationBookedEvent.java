package com.otapp.hmis.engine.encounter.consultation.application.event;

import com.otapp.hmis.engine.patient.domain.PaymentType;

/**
 * Published from {@code ConsultationService.book} once the consultation is
 * saved. Billing listens after-commit and seeds the consultation-fee invoice
 * (the legacy "send to doctor creates the consultation bill" step). Transfer
 * does NOT publish this — a transferred consultation inherits the source's
 * settled state and is not re-charged.
 */
public record ConsultationBookedEvent(String consultationUid,
                                      String patientUid,
                                      PaymentType paymentType,
                                      String insurancePlanUid,
                                      boolean followUp) {}
