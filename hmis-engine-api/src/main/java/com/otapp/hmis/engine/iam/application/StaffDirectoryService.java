package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.iam.application.dto.StaffOption;
import com.otapp.hmis.engine.iam.domain.ProviderProfile;
import com.otapp.hmis.engine.iam.domain.ProviderProfileRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only lookup of active staff by role, used by other modules (e.g.
 * encounter, masterdata) to populate clinician / nurse pickers and to validate
 * role membership without breaking the {@code com.otapp.hmis.engine.iam} module
 * boundary.
 */
@Service
@RequiredArgsConstructor
public class StaffDirectoryService {

    private final UserRepository userRepository;
    private final ProviderProfileRepository providerProfileRepository;

    @Transactional(readOnly = true)
    public List<StaffOption> findByRole(String roleName) {
        List<User> users = userRepository.findEnabledByRoleName(roleName);
        Map<String, ProviderProfile> profiles = providerProfileRepository
                .findAllByUserUidIn(users.stream().map(User::getUid).toList()).stream()
                .collect(Collectors.toMap(ProviderProfile::getUserUid, Function.identity()));
        return users.stream()
                .map(u -> toOption(u, profiles.get(u.getUid())))
                .toList();
    }

    /** Single staff lookup by username, used to enrich affiliation listings. */
    @Transactional(readOnly = true)
    public Optional<StaffOption> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(u -> toOption(u, providerProfileRepository.findByUserUid(u.getUid()).orElse(null)));
    }

    /** True if an <em>enabled</em> user with this username holds the given role. */
    @Transactional(readOnly = true)
    public boolean isUserInRole(String username, String roleName) {
        return userRepository.existsEnabledByUsernameAndRoleName(username, roleName);
    }

    private StaffOption toOption(User u, ProviderProfile profile) {
        return new StaffOption(
                u.getUid(),
                u.getUsername(),
                u.getFirstName(),
                u.getLastName(),
                u.fullName(),
                profile == null ? null : profile.getSpecialty(),
                profile == null ? null : profile.getRegistrationNo(),
                profile == null ? null : profile.getLicenseNo());
    }
}
