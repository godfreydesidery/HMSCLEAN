package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.iam.application.dto.StaffOption;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only lookup of active staff by role, used by other modules (e.g.
 * encounter) to populate clinician / nurse pickers without breaking the
 * {@code com.otapp.hmis.engine.iam} module boundary.
 */
@Service
@RequiredArgsConstructor
public class StaffDirectoryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<StaffOption> findByRole(String roleName) {
        return userRepository.findEnabledByRoleName(roleName).stream()
                .map(u -> new StaffOption(
                        u.getUid(),
                        u.getUsername(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.fullName()))
                .toList();
    }
}
