package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.iam.application.dto.CreateRoleRequest;
import com.otapp.hmis.engine.iam.application.dto.PrivilegeDto;
import com.otapp.hmis.engine.iam.application.dto.RoleDto;
import com.otapp.hmis.engine.iam.domain.Privilege;
import com.otapp.hmis.engine.iam.domain.PrivilegeRepository;
import com.otapp.hmis.engine.iam.domain.Role;
import com.otapp.hmis.engine.iam.domain.RoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    @Transactional
    public RoleDto create(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new ConflictException("Role already exists: " + request.name());
        }
        Role role = new Role(request.name(), request.description());
        attachPrivileges(role, request.privileges());
        roleRepository.save(role);
        return IamMapper.toDto(role);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> list() {
        return roleRepository.findAll().stream()
                .map(IamMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDto findByUid(String uid) {
        return roleRepository.findByUid(uid)
                .map(IamMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Role not found: " + uid));
    }

    @Transactional
    public RoleDto replacePrivileges(String uid, Set<String> privilegeNames) {
        Role role = roleRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Role not found: " + uid));
        role.getPrivileges().clear();
        attachPrivileges(role, privilegeNames);
        return IamMapper.toDto(role);
    }

    @Transactional(readOnly = true)
    public List<PrivilegeDto> listPrivileges() {
        return privilegeRepository.findAll().stream()
                .map(IamMapper::toDto)
                .toList();
    }

    private void attachPrivileges(Role role, Set<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        Set<Privilege> resolved = new HashSet<>();
        for (String name : names) {
            Privilege priv = privilegeRepository.findByName(name)
                    .orElseThrow(() -> new NotFoundException("Privilege not found: " + name));
            resolved.add(priv);
        }
        resolved.forEach(role::grant);
    }
}
