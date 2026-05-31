package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.iam.domain.Role;
import com.otapp.hmis.engine.iam.domain.RoleRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.infrastructure.bootstrap.IamBootstrapProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Protected-identity guards ported from the legacy {@code UserResource}
 * create/update/delete and {@code saveRole}/{@code updateRole} gates
 * (com.orbix.api).
 *
 * <p>Single-responsibility, side-effect-free assertions used by
 * {@link UserService} and {@link RoleService}. All guards operate on
 * already-loaded aggregates (no extra DB hits on the user path); the
 * reserved-name guard consults {@link RoleRepository} once on role creation.
 *
 * <h2>Faithful scope</h2>
 * <ul>
 *   <li><b>SELF (de)activation guard.</b> Legacy blocked a user toggling their
 *   OWN active flag ("A user can not deactivate/activate itself"). Reproduced
 *   here — applies to every user including root acting on themselves.</li>
 *   <li><b>Root via another admin.</b> Legacy PERMITTED another admin to toggle
 *   the root account active; we deliberately do NOT block that (faithful
 *   decision). Only the self-guard applies to enabled changes.</li>
 *   <li><b>Protected/system roles.</b> Legacy froze the system-role name set
 *   ("Editing this role is not allowed", "Role name not available"). Reproduced
 *   data-driven via {@link Role#isProtected()}: their privileges may not be
 *   replaced, and a new role may not collide with a protected/reserved name.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProtectedIdentityPolicy {

    /** Reserved super-role name, guarded independently of seeded data. */
    static final String ROOT_ROLE = "ROOT";

    private final RoleRepository roleRepository;
    private final IamBootstrapProperties bootstrapProperties;

    /**
     * SELF (de)activation guard. A user may not change their own enabled flag
     * (regardless of direction). Toggling another account — including root — is
     * permitted, matching legacy behaviour.
     *
     * @param target          the user whose enabled flag is being changed
     * @param desiredEnabled  the requested enabled value
     * @param currentUsername the authenticated caller's username (may be null
     *                        for unauthenticated/system contexts, in which case
     *                        no self-guard applies)
     */
    public void assertCanSetEnabled(User target, boolean desiredEnabled, String currentUsername) {
        if (currentUsername == null) {
            return;
        }
        boolean self = target.getUsername().equalsIgnoreCase(currentUsername);
        if (!self) {
            return;
        }
        // No-op same-state passes (legacy parity); only an actual flip is blocked.
        if (target.isEnabled() == desiredEnabled) {
            return;
        }
        if (target.isEnabled() && !desiredEnabled) {
            throw new BusinessRuleException("A user can not deactivate itself");
        }
        throw new BusinessRuleException("A user can not activate itself");
    }

    /**
     * Block mutation of a protected/system role's privileges. Legacy rejected
     * any edit of a frozen system role; the rewrite only exposes privilege
     * replacement, so that is the operation guarded here.
     */
    public void assertCanReplacePrivileges(Role role) {
        if (role.isProtected()) {
            throw new BusinessRuleException(
                    "System role '" + role.getName() + "' is protected and its privileges cannot be modified");
        }
    }

    /**
     * Reject creating a role whose name collides with a reserved/protected
     * system-role name (case-insensitive). Distinct from the plain uniqueness
     * check: this also forbids names that map onto a protected role and the
     * literal ROOT super-role even if (somehow) not yet present.
     */
    public void assertRoleNameAvailable(String name) {
        if (name == null) {
            return;
        }
        String trimmed = name.trim();
        if (trimmed.equalsIgnoreCase(ROOT_ROLE)) {
            throw new ConflictException("Role name not available: " + name);
        }
        roleRepository.findByNameIgnoreCase(trimmed)
                .filter(Role::isProtected)
                .ifPresent(r -> {
                    throw new ConflictException("Role name not available: " + name);
                });
    }

    /** @return the configured root username (the super-admin account). */
    String rootUsername() {
        return bootstrapProperties.rootUsername();
    }
}
