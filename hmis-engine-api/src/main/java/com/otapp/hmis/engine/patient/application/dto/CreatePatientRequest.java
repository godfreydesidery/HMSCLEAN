package com.otapp.hmis.engine.patient.application.dto;

import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.PatientType;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePatientRequest(
        @NotBlank @Size(max = 80)  String firstName,
        @Size(max = 80)            String middleName,
        @NotBlank @Size(max = 80)  String lastName,

        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotNull PatientType type,
        @NotNull PaymentType paymentType,

        @Size(min = 26, max = 26) String insurancePlanUid,
        @Size(max = 64)  String membershipNo,

        @Size(max = 40)  String phoneNo,
        @Email @Size(max = 120) String email,
        @Size(max = 255) String address,
        @Size(max = 80)  String nationality,
        @Size(max = 64)  String nationalId,
        @Size(max = 64)  String passportNo,

        @Size(max = 160) String kinFullName,
        @Size(max = 80)  String kinRelationship,
        @Size(max = 40)  String kinPhoneNo) {
}
