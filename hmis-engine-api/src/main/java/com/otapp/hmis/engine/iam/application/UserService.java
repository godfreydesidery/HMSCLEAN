package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.iam.application.dto.CreateUserRequest;
import com.otapp.hmis.engine.iam.application.dto.UserSummary;
import com.otapp.hmis.engine.iam.domain.Role;
import com.otapp.hmis.engine.iam.domain.RoleRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
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
    public PageResponse<UserSummary> list(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(IamMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public UserSummary findByUid(String uid) {
        return userRepository.findByUid(uid)
                .map(IamMapper::toSummary)
                .orElseThrow(() -> new NotFoundException("User not found: " + uid));
    }

    @Transactional
    public UserSummary setEnabled(String uid, boolean enabled) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("User not found: " + uid));
        user.setEnabled(enabled);
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
