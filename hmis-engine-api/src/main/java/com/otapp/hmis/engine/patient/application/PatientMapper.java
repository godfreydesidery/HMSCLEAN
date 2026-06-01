package com.otapp.hmis.engine.patient.application;

import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.patient.application.dto.PatientDto;
import com.otapp.hmis.engine.patient.application.dto.PatientSummary;
import com.otapp.hmis.engine.patient.domain.Patient;

final class PatientMapper {

    private PatientMapper() {
    }

    static PatientSummary toSummary(Patient p) {
        return new PatientSummary(
                p.getUid(),
                p.getPatientNo(),
                p.getFirstName(),
                p.getMiddleName(),
                p.getLastName(),
                p.getDateOfBirth(),
                p.getGender(),
                p.getType(),
                p.getPaymentType(),
                p.getPhoneNo(),
                p.getLastVisitAt(),
                p.isActive());
    }

    static PatientDto toDto(Patient p, InsurancePlan plan, String providerName) {
        return new PatientDto(
                p.getUid(),
                p.getPatientNo(),
                p.getFirstName(),
                p.getMiddleName(),
                p.getLastName(),
                p.getDateOfBirth(),
                p.getGender(),
                p.getType(),
                p.getPaymentType(),
                p.getInsurancePlanUid(),
                plan == null ? null : plan.getName(),
                providerName,
                p.getMembershipNo(),
                p.getPhoneNo(),
                p.getEmail(),
                p.getAddress(),
                p.getNationality(),
                p.getNationalId(),
                p.getPassportNo(),
                p.getKinFullName(),
                p.getKinRelationship(),
                p.getKinPhoneNo(),
                p.getKin2FullName(),
                p.getKin2Relationship(),
                p.getKin2PhoneNo(),
                p.getKin3FullName(),
                p.getKin3Relationship(),
                p.getKin3PhoneNo(),
                p.getLastVisitAt(),
                p.isActive(),
                p.isDeceased(),
                p.getDeceasedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
