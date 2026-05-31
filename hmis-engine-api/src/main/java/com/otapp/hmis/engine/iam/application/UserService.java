package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.iam.application.dto.CreateUserRequest;
import com.otapp.hmis.engine.iam.application.dto.UserSummary;
import com.otapp.hmis.engine.iam.domain.Role;
import com.otapp.hmis.engine.iam.domain.RoleRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProtectedIdentityPolicy protectedIdentityPolicy;

    @Transactional
    public UserSummary create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists: " + request.username());
        }
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.email());
        attachRoles(user, request.roles());
        userRepository.save(user);
        return IamMapper.toSummary(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummary> search(String query, Boolean enabled, Pageable pageable) {
        return PageResponse.from(
                userRepository.search(query == null ? null : query.trim(), enabled, pageable)
                        .map(IamMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public UserSummary findByUid(String uid) {
        return userRepository.findByUid(uid)
                .map(IamMapper::toSummary)
                .orElseThrow(() -> new NotFoundException("User not found: " + uid));
    }

    /**
     * Enable or disable a user account.
     *
     * <p>Guarded by the legacy SELF (de)activation rule: a user may not change
     * their own enabled flag. {@code currentUsername} is the authenticated
     * caller threaded in from the controller; pass {@code null} for system
     * contexts where no self-guard applies.
     */
    @Transactional
    public UserSummary setEnabled(String uid, boolean enabled, String currentUsername) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("User not found: " + uid));
        protectedIdentityPolicy.assertCanSetEnabled(user, enabled, currentUsername);
        user.setEnabled(enabled);
        return IamMapper.toSummary(user);
    }

    /**
     * Admin password reset. Sets a temporary password and flags the user
     * so the next login forces a self-service change. Clears any lockout
     * so the user can sign in immediately with the temporary credentials.
     */
    @Transactional
    public UserSummary resetPassword(String uid, String newPassword) {
        if (newPassword == null || newPassword.length() < SecurityPolicy.MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException("Password must be at least "
                    + SecurityPolicy.MIN_PASSWORD_LENGTH + " characters long");
        }
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("User not found: " + uid));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setPasswordMustChange(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return IamMapper.toSummary(user);
    }

    /**
     * Admin unlock — clears any active lockout and resets the failed-attempt counter.
     */
    @Transactional
    public UserSummary unlock(String uid) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("User not found: " + uid));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return IamMapper.toSummary(user);
    }

    @Transactional
    public UserSummary replaceRoles(String uid, Set<String> roleNames) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("User not found: " + uid));
        user.getRoles().clear();
        attachRoles(user, roleNames);
        return IamMapper.toSummary(user);
    }

    private void attachRoles(User user, Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        Set<Role> resolved = new HashSet<>();
        for (String name : roleNames) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new NotFoundException("Role not found: " + name));
            resolved.add(role);
        }
        resolved.forEach(user::grant);
    }
}
