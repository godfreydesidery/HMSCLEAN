package com.otapp.hmis.engine.iam.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "iam_user", uniqueConstraints = @UniqueConstraint(name = "uk_iam_user_username", columnNames = "username"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Setter
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Setter
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Setter
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Setter
    @Column(length = 120)
    private String email;

    @Setter
    @Column(nullable = false)
    private boolean enabled = true;

    /** Set to true after an admin reset; user must change password before doing anything else. */
    @Setter
    @Column(name = "password_must_change", nullable = false)
    private boolean passwordMustChange = false;

    @Setter
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    /** Counter of consecutive failed login attempts since last success. Reset on successful login. */
    @Setter
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    /** Account is locked while {@code locked_until > now()}. */
    @Setter
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Setter
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "iam_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_iam_user_role", columnNames = {"user_id", "role_id"}))
    private Set<Role> roles = new HashSet<>();

    public User(String username, String passwordHash, String firstName, String lastName, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public void grant(Role role) {
        roles.add(role);
    }

    public void revoke(Role role) {
        roles.remove(role);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }
}
