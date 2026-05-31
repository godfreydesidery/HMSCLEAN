package com.otapp.hmis.engine.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.iam.domain.Role;
import com.otapp.hmis.engine.iam.domain.RoleRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.infrastructure.bootstrap.IamBootstrapProperties;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit coverage of the legacy-faithful protected-identity guards
 * (ported from com.orbix.api UserResource create/update/delete and
 * saveRole/updateRole). No Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class ProtectedIdentityPolicyTest {

    @Mock
    private RoleRepository roleRepository;

    private ProtectedIdentityPolicy policy;

    @BeforeEach
    void setUp() {
        IamBootstrapProperties props = new IamBootstrapProperties("root", "secret");
        policy = new ProtectedIdentityPolicy(roleRepository, props);
    }

    private static User user(String username, boolean enabled) {
        User u = new User(username, "hash", "First", "Last", null);
        u.setEnabled(enabled);
        return u;
    }

    // --- SELF (de)activation guard ------------------------------------------

    @Test
    void blocksSelfDeactivation() {
        User self = user("alice", true);
        assertThatThrownBy(() -> policy.assertCanSetEnabled(self, false, "alice"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A user can not deactivate itself");
    }

    @Test
    void blocksSelfActivation() {
        User self = user("alice", false);
        assertThatThrownBy(() -> policy.assertCanSetEnabled(self, true, "alice"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A user can not activate itself");
    }

    @Test
    void selfMatchIsCaseInsensitive() {
        User self = user("Alice", true);
        assertThatThrownBy(() -> policy.assertCanSetEnabled(self, false, "alice"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void noOpSameStateOnSelfPasses() {
        User self = user("alice", true);
        assertThatCode(() -> policy.assertCanSetEnabled(self, true, "alice")).doesNotThrowAnyException();
    }

    @Test
    void anotherAdminMayToggleAnyAccount() {
        User target = user("bob", true);
        assertThatCode(() -> policy.assertCanSetEnabled(target, false, "alice")).doesNotThrowAnyException();
    }

    @Test
    void anotherAdminMayDisableRootFaithfulLegacyAllows() {
        // Legacy PERMITTED another admin to toggle root active; we do not block it.
        User root = user("root", true);
        assertThatCode(() -> policy.assertCanSetEnabled(root, false, "alice")).doesNotThrowAnyException();
    }

    @Test
    void rootStillCannotDeactivateItself() {
        User root = user("root", true);
        assertThatThrownBy(() -> policy.assertCanSetEnabled(root, false, "root"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A user can not deactivate itself");
    }

    @Test
    void nullCurrentUsernameSkipsGuard() {
        User self = user("alice", true);
        assertThatCode(() -> policy.assertCanSetEnabled(self, false, null)).doesNotThrowAnyException();
    }

    // --- Protected/system role guard ----------------------------------------

    @Test
    void blocksReplacingPrivilegesOnProtectedRole() {
        Role protectedRole = mock(Role.class);
        when(protectedRole.isProtected()).thenReturn(true);
        when(protectedRole.getName()).thenReturn("CLINICIAN");
        assertThatThrownBy(() -> policy.assertCanReplacePrivileges(protectedRole))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CLINICIAN")
                .hasMessageContaining("protected");
    }

    @Test
    void allowsReplacingPrivilegesOnNonProtectedRole() {
        Role custom = mock(Role.class);
        when(custom.isProtected()).thenReturn(false);
        assertThatCode(() -> policy.assertCanReplacePrivileges(custom)).doesNotThrowAnyException();
    }

    // --- Reserved role-name guard -------------------------------------------

    @Test
    void rejectsRootNameRegardlessOfData() {
        assertThatThrownBy(() -> policy.assertRoleNameAvailable("root"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsCollisionWithExistingProtectedRoleCaseInsensitive() {
        Role seeded = mock(Role.class);
        lenient().when(seeded.isProtected()).thenReturn(true);
        when(roleRepository.findByNameIgnoreCase("clinician")).thenReturn(Optional.of(seeded));
        assertThatThrownBy(() -> policy.assertRoleNameAvailable("clinician"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void allowsNameThatCollidesOnlyWithNonProtectedRole() {
        Role nonProtected = mock(Role.class);
        when(nonProtected.isProtected()).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase("WARD_CLERK")).thenReturn(Optional.of(nonProtected));
        assertThatCode(() -> policy.assertRoleNameAvailable("WARD_CLERK")).doesNotThrowAnyException();
    }

    @Test
    void allowsBrandNewName() {
        when(roleRepository.findByNameIgnoreCase("WARD_CLERK")).thenReturn(Optional.empty());
        assertThatCode(() -> policy.assertRoleNameAvailable("WARD_CLERK")).doesNotThrowAnyException();
    }

    @Test
    void exposesConfiguredRootUsername() {
        assertThat(policy.rootUsername()).isEqualTo("root");
    }
}
