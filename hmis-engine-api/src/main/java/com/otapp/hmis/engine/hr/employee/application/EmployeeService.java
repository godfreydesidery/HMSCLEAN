package com.otapp.hmis.engine.hr.employee.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderRepository;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.ClinicianPerformanceDto;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.CreateEmployeeRequest;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.EmployeeDto;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.SetStatusRequest;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.TerminateEmployeeRequest;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.UpdateEmployeeRequest;
import com.otapp.hmis.engine.hr.employee.domain.Employee;
import com.otapp.hmis.engine.hr.employee.domain.EmployeeRepository;
import com.otapp.hmis.engine.hr.employee.domain.EmploymentStatus;
import com.otapp.hmis.engine.hr.employee.infrastructure.EmployeeNumberGenerator;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final EmployeeNumberGenerator numberGenerator;
    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;
    private final AdmissionRepository admissionRepository;
    private final ClinicalOrderRepository orderRepository;

    @Transactional
    public EmployeeDto create(CreateEmployeeRequest request) {
        String username = emptyToNull(request.username());
        if (username != null) {
            validateUsernameLink(username);
        }
        Employee e = new Employee(
                numberGenerator.next(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.hireDate(),
                emptyToNull(request.designation()),
                emptyToNull(request.department()));
        applyCommonFields(e, request.middleName(), request.gender(), request.dateOfBirth(),
                request.nationalId(), request.phone(), request.email(), request.address(), username);
        repository.save(e);
        return toDto(e);
    }

    @Transactional
    public EmployeeDto update(String uid, UpdateEmployeeRequest request) {
        Employee e = loadOrThrow(uid);
        String username = emptyToNull(request.username());
        if (username != null && !username.equals(e.getUsername())) {
            validateUsernameLink(username);
        }
        e.setFirstName(request.firstName().trim());
        e.setLastName(request.lastName().trim());
        e.setDesignation(emptyToNull(request.designation()));
        e.setDepartment(emptyToNull(request.department()));
        applyCommonFields(e, request.middleName(), request.gender(), request.dateOfBirth(),
                request.nationalId(), request.phone(), request.email(), request.address(), username);
        return toDto(e);
    }

    @Transactional
    public EmployeeDto setStatus(String uid, SetStatusRequest request) {
        Employee e = loadOrThrow(uid);
        if (request.status() == EmploymentStatus.TERMINATED) {
            throw new BusinessRuleException("Use the /terminate endpoint to terminate an employee");
        }
        if (e.getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            throw new BusinessRuleException("Cannot change status of a TERMINATED employee");
        }
        e.setEmploymentStatus(request.status());
        return toDto(e);
    }

    @Transactional
    public EmployeeDto terminate(String uid, TerminateEmployeeRequest request) {
        Employee e = loadOrThrow(uid);
        e.terminate(request.terminationDate(), emptyToNull(request.reason()));
        return toDto(e);
    }

    @Transactional(readOnly = true)
    public EmployeeDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDto> search(String query, EmploymentStatus status,
                                            String designation, String department,
                                            Pageable pageable) {
        return PageResponse.from(
                repository.search(emptyToNull(query), status,
                                emptyToNull(designation), emptyToNull(department), pageable)
                        .map(EmployeeService::toDto));
    }

    /**
     * Rolls up the encounters + orders a clinician handled in a date
     * range (PROCESS.md §12 — clinician performance). Counts based on:
     *   - consultations: bookedAt + clinicianUsername
     *   - admissions:   admittedAt + admittingClinicianUsername
     *   - orders:       requestedAt + createdBy audit field
     */
    @Transactional(readOnly = true)
    public ClinicianPerformanceDto clinicianPerformance(String uid, LocalDate from, LocalDate to) {
        Employee e = loadOrThrow(uid);
        if (e.getUsername() == null) {
            throw new BusinessRuleException(
                    "Employee has no system username; clinician performance is only meaningful for staff with a User account");
        }
        if (from == null || to == null) {
            throw new BusinessRuleException("from and to dates are required");
        }
        if (to.isBefore(from)) {
            throw new BusinessRuleException("to cannot be before from");
        }
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(); // exclusive upper bound

        String username = e.getUsername();
        long consultations  = consultationRepository.countByClinicianInRange(username, fromInstant, toInstant);
        long admissions     = admissionRepository.countByAdmittingClinicianInRange(username, fromInstant, toInstant);
        long labOrders      = orderRepository.countByKindAndCreatedByInRange(
                ClinicalOrderKind.LAB_TEST,  username, fromInstant, toInstant);
        long radiologyOrders = orderRepository.countByKindAndCreatedByInRange(
                ClinicalOrderKind.RADIOLOGY, username, fromInstant, toInstant);
        long procedureOrders = orderRepository.countByKindAndCreatedByInRange(
                ClinicalOrderKind.PROCEDURE, username, fromInstant, toInstant);

        return new ClinicianPerformanceDto(
                username, from, to,
                consultations, admissions,
                labOrders, radiologyOrders, procedureOrders);
    }

    // ----- helpers ---------------------------------------------------------

    private Employee loadOrThrow(String uid) {
        return repository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Employee not found: " + uid));
    }

    private void validateUsernameLink(String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        if (repository.existsByUsername(username)) {
            throw new ConflictException("Username already linked to another employee: " + username);
        }
    }

    private static void applyCommonFields(Employee e, String middleName, String gender,
                                          LocalDate dateOfBirth, String nationalId, String phone,
                                          String email, String address, String username) {
        e.setMiddleName(emptyToNull(middleName));
        e.setGender(emptyToNull(gender));
        e.setDateOfBirth(dateOfBirth);
        e.setNationalId(emptyToNull(nationalId));
        e.setPhone(emptyToNull(phone));
        e.setEmail(emptyToNull(email));
        e.setAddress(emptyToNull(address));
        e.setUsername(username);
    }

    private static EmployeeDto toDto(Employee e) {
        return new EmployeeDto(
                e.getUid(), e.getEmployeeNo(),
                e.getFirstName(), e.getMiddleName(), e.getLastName(), e.fullName(),
                e.getGender(), e.getDateOfBirth(), e.getNationalId(),
                e.getPhone(), e.getEmail(), e.getAddress(),
                e.getUsername(), e.getDesignation(), e.getDepartment(),
                e.getHireDate(),
                e.getEmploymentStatus(),
                e.getTerminationDate(), e.getTerminationReason(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
