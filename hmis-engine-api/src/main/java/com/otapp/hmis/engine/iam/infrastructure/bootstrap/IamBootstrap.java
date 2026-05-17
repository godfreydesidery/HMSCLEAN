package com.otapp.hmis.engine.iam.infrastructure.bootstrap;

import com.otapp.hmis.engine.iam.domain.Role;
import com.otapp.hmis.engine.iam.domain.RoleRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures a usable root user exists on first startup.
 *
 * <p>The role and privilege catalog is loaded by Flyway migration V2; this
 * runner only seeds the {@code root} user (with a hashed password) so the
 * deployment has a way in. Operators must change the password immediately.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(IamBootstrapProperties.class)
@RequiredArgsConstructor
public class IamBootstrap implements CommandLineRunner {

    private static final String ROOT_ROLE = "ROOT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamBootstrapProperties properties;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername(properties.rootUsername())) {
            return;
        }
        Role rootRole = roleRepository.findByName(ROOT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Expected " + ROOT_ROLE + " role from migration V2"));

        User root = new User(
                properties.rootUsername(),
                passwordEncoder.encode(properties.rootPassword()),
                "Root",
                "Administrator",
                null);
        root.grant(rootRole);
        userRepository.save(root);

        log.info("Bootstrapped root user '{}'. Change the password on first login.", properties.rootUsername());
    }
}
