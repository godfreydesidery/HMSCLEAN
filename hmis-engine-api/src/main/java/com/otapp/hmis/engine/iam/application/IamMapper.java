package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.iam.application.dto.PrivilegeDto;
import com.otapp.hmis.engine.iam.application.dto.RoleDto;
import com.otapp.hmis.engine.iam.application.dto.UserSummary;
import com.otapp.hmis.engine.iam.domain.Privilege;
import com.otapp.hmis.engine.iam.domain.Role;
import com.otapp.hmis.engine.iam.domain.User;
import java.util.Comparator;
import java.util.List;

final class IamMapper {

    private IamMapper() {
    }

    static UserSummary toSummary(User user) {
        return new UserSummary(
                user.getUid(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isEnabled(),
                user.isLocked(),
                user.isPasswordMustChange(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                roleNames(user));
    }

    static RoleDto toDto(Role role) {
        List<String> privileges = role.getPrivileges().stream()
                .map(Privilege::getName)
                .sorted()
                .toList();
        return new RoleDto(role.getUid(), role.getName(), role.getDescription(), privileges);
    }

    static PrivilegeDto toDto(Privilege privilege) {
        return new PrivilegeDto(privilege.getUid(), privilege.getName(), privilege.getDescription());
    }

    static List<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
    }

    static List<String> privilegeNames(User user) {
        return user.getRoles().stream()
                .flatMap(r -> r.getPrivileges().stream())
                .map(Privilege::getName)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
