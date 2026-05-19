package com.otapp.hmis.engine.patient.application.dto;

import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.PatientType;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.time.Instant;
import java.time.LocalDate;

public record PatientSummary(
        String uid,
        String patientNo,
        String firstName,
        String middleName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        PatientType type,
        PaymentType paymentType,
        String phoneNo,
        Instant lastVisitAt,
        boolean active) {
}
