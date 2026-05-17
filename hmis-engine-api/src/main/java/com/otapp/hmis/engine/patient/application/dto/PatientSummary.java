package com.otapp.hmis.engine.patient.application.dto;

import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.time.LocalDate;

public record PatientSummary(
        String uid,
        String patientNo,
        String firstName,
        String middleName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        PaymentType paymentType,
        String phoneNo,
        boolean active) {
}
