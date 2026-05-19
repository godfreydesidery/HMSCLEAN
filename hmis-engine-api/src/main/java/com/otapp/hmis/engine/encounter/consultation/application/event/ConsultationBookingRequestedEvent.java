package com.otapp.hmis.engine.encounter.consultation.application.event;

import com.otapp.hmis.engine.patient.domain.PaymentType;

/**
 * Published synchronously from {@code ConsultationService.book} before the
 * consultation is saved. Billing's gate listener fires in the same
 * transaction and throws if the patient has any unpaid registration invoice
 * (CASH patients only) — that exception aborts the booking.
 */
public record ConsultationBookingRequestedEvent(String patientUid, PaymentType paymentType) {}
