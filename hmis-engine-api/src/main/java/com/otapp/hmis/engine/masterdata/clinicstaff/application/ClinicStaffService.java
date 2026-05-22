package com.otapp.hmis.engine.masterdata.clinicstaff.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.iam.application.StaffDirectoryService;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.clinicstaff.application.dto.ClinicClinicianDto;
import com.otapp.hmis.engine.masterdata.clinicstaff.domain.ClinicClinician;
import com.otapp.hmis.engine.masterdata.clinicstaff.domain.ClinicClinicianRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the clinician ⇄ clinic affiliation. Assignment validates the user
 * against {@code iam} (must exist, be enabled and hold the {@code CLINICIAN}
 * role); {@link #isAssigned(String, String)} is the read-only gate consumed by
 * the {@code encounter} module at consultation booking.
 */
@Service
@RequiredArgsConstructor
public class ClinicStaffService {

    static final String CLINICIAN_ROLE = "CLINICIAN";

    private final ClinicClinicianRepository repository;
    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final StaffDirectoryService staffDirectory;

    /** Idempotent: re-assigning an existing (possibly removed) clinician reactivates the row. */
    @Transactional
    public ClinicClinicianDto assignClinician(String clinicUid, String userUid) {
        Clinic clinic = clinicRepository.findByUid(clinicUid)
                .orElseThrow(() -> new NotFoundException("Clinic not found: " + clinicUid));
        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new NotFoundException("User not found: " + userUid));
        if (!user.isEnabled()) {
            throw new BusinessRuleException("User account is disabled: " + user.getUsername());
        }
        if (!staffDirectory.isUserInRole(user.getUsername(), CLINICIAN_ROLE)) {
            throw new BusinessRuleException("User " + user.getUsername() + " is not a clinician");
        }

        ClinicClinician row = repository.findByClinicUidAndUserUid(clinic.getUid(), user.getUid())
                .map(existing -> {
                    existing.activate();
                    return existing;
                })
                .orElseGet(() -> repository.save(
                        new ClinicClinician(clinic.getUid(), user.getUid(), user.getUsername())));
        return ClinicStaffMapper.toDto(row, staffDirectory.findByUsername(row.getUsername()).orElse(null));
    }

    @Transactional
    public void removeClinician(String clinicUid, String userUid) {
        ClinicClinician row = repository.findByClinicUidAndUserUid(clinicUid, userUid)
                .orElseThrow(() -> new NotFoundException(
                        "Clinician " + userUid + " is not assigned to clinic " + clinicUid));
        row.deactivate();
    }

    @Transactional(readOnly = true)
    public List<ClinicClinicianDto> listClinicians(String clinicUid) {
        return repository.findByClinicUidAndActiveTrueOrderByUsername(clinicUid).stream()
                .map(row -> ClinicStaffMapper.toDto(
                        row, staffDirectory.findByUsername(row.getUsername()).orElse(null)))
                .toList();
    }

    /** Booking gate: is this clinician currently affiliated with the clinic? */
    @Transactional(readOnly = true)
    public boolean isAssigned(String clinicUid, String username) {
        return repository.existsByClinicUidAndUsernameAndActiveTrue(clinicUid, username);
    }
}
