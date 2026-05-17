package com.otapp.hmis.engine.patient.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsuranceProvider;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsuranceProviderRepository;
import com.otapp.hmis.engine.patient.application.dto.CreatePatientRequest;
import com.otapp.hmis.engine.patient.application.dto.PatientDto;
import com.otapp.hmis.engine.patient.application.dto.PatientSummary;
import com.otapp.hmis.engine.patient.application.dto.UpdatePatientRequest;
import com.otapp.hmis.engine.patient.domain.Gender;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import com.otapp.hmis.engine.patient.infrastructure.PatientNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final PatientNumberGenerator patientNumberGenerator;

    @Transactional
    public PatientDto register(CreatePatientRequest request) {
        validateInsuranceForPayment(request.paymentType(), request.insurancePlanUid());

        Patient patient = new Patient(
                patientNumberGenerator.next(),
                request.firstName().trim(),
                emptyToNull(request.middleName()),
                request.lastName().trim(),
                request.dateOfBirth(),
                request.gender(),
                request.type(),
                request.paymentType());

        applyContactAndKin(patient, request);
        patient.setInsurancePlanUid(emptyToNull(request.insurancePlanUid()));
        patient.setMembershipNo(emptyToNull(request.membershipNo()));

        patientRepository.save(patient);
        return toDto(patient);
    }

    @Transactional
    public PatientDto update(String uid, UpdatePatientRequest request) {
        validateInsuranceForPayment(request.paymentType(), request.insurancePlanUid());

        Patient patient = loadOrThrow(uid);
        patient.setFirstName(request.firstName().trim());
        patient.setMiddleName(emptyToNull(request.middleName()));
        patient.setLastName(request.lastName().trim());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setType(request.type());
        patient.setPaymentType(request.paymentType());
        patient.setInsurancePlanUid(emptyToNull(request.insurancePlanUid()));
        patient.setMembershipNo(emptyToNull(request.membershipNo()));

        applyContactAndKin(patient, request);
        return toDto(patient);
    }

    @Transactional
    public PatientDto setActive(String uid, boolean active) {
        Patient patient = loadOrThrow(uid);
        if (active) patient.activate(); else patient.deactivate();
        return toDto(patient);
    }

    @Transactional(readOnly = true)
    public PatientDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientSummary> search(String query, Boolean active, Gender gender,
                                               PaymentType paymentType, Pageable pageable) {
        String trimmed = query == null ? null : query.trim();
        return PageResponse.from(
                patientRepository.search(trimmed, active, gender, paymentType, pageable)
                        .map(PatientMapper::toSummary));
    }

    private Patient loadOrThrow(String uid) {
        return patientRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + uid));
    }

    private void validateInsuranceForPayment(PaymentType paymentType, String insurancePlanUid) {
        boolean needsPlan = paymentType == PaymentType.INSURANCE || paymentType == PaymentType.MIXED;
        boolean hasPlan = insurancePlanUid != null && !insurancePlanUid.isBlank();
        if (needsPlan && !hasPlan) {
            throw new BusinessRuleException("Insurance plan is required for payment type " + paymentType);
        }
        if (hasPlan) {
            insurancePlanRepository.findByUid(insurancePlanUid)
                    .orElseThrow(() -> new NotFoundException("Insurance plan not found: " + insurancePlanUid));
        }
    }

    private void applyContactAndKin(Patient patient, CreatePatientRequest r) {
        patient.setPhoneNo(emptyToNull(r.phoneNo()));
        patient.setEmail(emptyToNull(r.email()));
        patient.setAddress(emptyToNull(r.address()));
        patient.setNationality(emptyToNull(r.nationality()));
        patient.setNationalId(emptyToNull(r.nationalId()));
        patient.setPassportNo(emptyToNull(r.passportNo()));
        patient.setKinFullName(emptyToNull(r.kinFullName()));
        patient.setKinRelationship(emptyToNull(r.kinRelationship()));
        patient.setKinPhoneNo(emptyToNull(r.kinPhoneNo()));
    }

    private void applyContactAndKin(Patient patient, UpdatePatientRequest r) {
        patient.setPhoneNo(emptyToNull(r.phoneNo()));
        patient.setEmail(emptyToNull(r.email()));
        patient.setAddress(emptyToNull(r.address()));
        patient.setNationality(emptyToNull(r.nationality()));
        patient.setNationalId(emptyToNull(r.nationalId()));
        patient.setPassportNo(emptyToNull(r.passportNo()));
        patient.setKinFullName(emptyToNull(r.kinFullName()));
        patient.setKinRelationship(emptyToNull(r.kinRelationship()));
        patient.setKinPhoneNo(emptyToNull(r.kinPhoneNo()));
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PatientDto toDto(Patient p) {
        InsurancePlan plan = p.getInsurancePlanUid() == null
                ? null
                : insurancePlanRepository.findByUid(p.getInsurancePlanUid()).orElse(null);
        String providerName = plan == null
                ? null
                : insuranceProviderRepository.findByUid(plan.getProviderUid())
                        .map(InsuranceProvider::getName)
                        .orElse(null);
        return PatientMapper.toDto(p, plan, providerName);
    }
}
