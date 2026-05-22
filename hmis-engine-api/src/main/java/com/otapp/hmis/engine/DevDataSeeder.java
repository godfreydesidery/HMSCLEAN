package com.otapp.hmis.engine;

import com.otapp.hmis.engine.iam.application.ProviderProfileService;
import com.otapp.hmis.engine.iam.application.dto.ProviderProfileDtos.UpsertProviderProfileRequest;
import com.otapp.hmis.engine.iam.domain.RoleRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicRepository;
import com.otapp.hmis.engine.masterdata.clinicstaff.application.ClinicStaffService;
import com.otapp.hmis.engine.masterdata.store.application.StoreStaffService;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local-profile convenience seeder. With the clinician ⇄ clinic and store
 * keeper ⇄ store affiliation gates in place, a fresh database has no affiliated
 * staff, so a consultation cannot be booked and store goods cannot be issued
 * until an admin sets things up. This runner provisions a sample clinician
 * (affiliated with every seeded clinic) and a sample store keeper (affiliated
 * with the main store) so the running app is usable for QA out of the box.
 *
 * <p>Gated to the {@code local} profile only — it never runs under {@code test}
 * (integration tests provision their own staff) or {@code prod} (operators set
 * up real staff via the admin UI).
 */
@Slf4j
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final String MAIN_STORE_UID = "01J5KQRPCD0000000000000ST1";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClinicRepository clinicRepository;
    private final ClinicStaffService clinicStaffService;
    private final StoreRepository storeRepository;
    private final StoreStaffService storeStaffService;
    private final ProviderProfileService providerProfileService;

    @Override
    @Transactional
    public void run(String... args) {
        seedClinician();
        seedStoreKeeper();
    }

    private void seedClinician() {
        User clinician = ensureUser("clinician", "Sample", "Clinician", "CLINICIAN");
        for (String code : List.of("OPD", "PED", "ER")) {
            clinicRepository.findByCode(code).ifPresent(c ->
                    clinicStaffService.assignClinician(c.getUid(), clinician.getUid()));
        }
        providerProfileService.upsert(clinician.getUid(),
                new UpsertProviderProfileRequest("General Practice", "REG-0001", "LIC-0001"));
        log.info("Dev seed: clinician '{}' affiliated with seeded clinics.", clinician.getUsername());
    }

    private void seedStoreKeeper() {
        User keeper = ensureUser("storekeeper", "Sample", "Keeper", "STORE_PERSON");
        storeRepository.findByUid(MAIN_STORE_UID).ifPresent(s ->
                storeStaffService.assign(s.getUid(), keeper.getUid()));
        log.info("Dev seed: store keeper '{}' affiliated with the main store.", keeper.getUsername());
    }

    private User ensureUser(String username, String firstName, String lastName, String roleName) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User(username, passwordEncoder.encode("ChangeMe!123"),
                    firstName, lastName, username + "@hmis.local");
            roleRepository.findByName(roleName).ifPresent(u::grant);
            return userRepository.save(u);
        });
    }
}
