package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.iam.application.dto.ProviderProfileDtos.ProviderProfileDto;
import com.otapp.hmis.engine.iam.application.dto.ProviderProfileDtos.UpsertProviderProfileRequest;
import com.otapp.hmis.engine.iam.domain.ProviderProfile;
import com.otapp.hmis.engine.iam.domain.ProviderProfileRepository;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the optional clinical-identity sidecar ({@link ProviderProfile}) for a
 * user. Restores the legacy {@code Clinician.type}/registration without
 * widening {@code iam_user}.
 */
@Service
@RequiredArgsConstructor
public class ProviderProfileService {

    private final ProviderProfileRepository providerProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProviderProfileDto upsert(String userUid, UpsertProviderProfileRequest request) {
        if (userRepository.findByUid(userUid).isEmpty()) {
            throw new NotFoundException("User not found: " + userUid);
        }
        ProviderProfile profile = providerProfileRepository.findByUserUid(userUid)
                .map(existing -> {
                    existing.setSpecialty(trimToNull(request.specialty()));
                    existing.setRegistrationNo(trimToNull(request.registrationNo()));
                    existing.setLicenseNo(trimToNull(request.licenseNo()));
                    return existing;
                })
                .orElseGet(() -> providerProfileRepository.save(new ProviderProfile(
                        userUid,
                        trimToNull(request.specialty()),
                        trimToNull(request.registrationNo()),
                        trimToNull(request.licenseNo()))));
        return toDto(profile);
    }

    @Transactional(readOnly = true)
    public ProviderProfileDto findByUserUid(String userUid) {
        return providerProfileRepository.findByUserUid(userUid)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("No provider profile for user: " + userUid));
    }

    private ProviderProfileDto toDto(ProviderProfile p) {
        return new ProviderProfileDto(
                p.getUid(),
                p.getUserUid(),
                p.getSpecialty(),
                p.getRegistrationNo(),
                p.getLicenseNo(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
