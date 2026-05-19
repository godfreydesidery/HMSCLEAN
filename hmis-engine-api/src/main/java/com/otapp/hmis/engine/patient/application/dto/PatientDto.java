package com.otapp.hmis.engine.patient.application.dto;

import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.PatientType;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import java.time.Instant;
import java.time.LocalDate;

public record PatientDto(
        String uid,
        String patientNo,

        String firstName,
        String middleName,
        String lastName,

        LocalDate dateOfBirth,
        Gender gender,
        PatientType type,
        PaymentType paymentType,

        String insurancePlanUid,
        String insurancePlanName,
        String insuranceProviderName,
        String membershipNo,

        String phoneNo,
        String email,
        String address,
        String nationality,
        String nationalId,
        String passportNo,

        String kinFullName,
        String kinRelationship,
        String kinPhoneNo,

        String kin2FullName,
        String kin2Relationship,
        String kin2PhoneNo,

        String kin3FullName,
        String kin3Relationship,
        String kin3PhoneNo,

        Instant lastVisitAt,

        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
