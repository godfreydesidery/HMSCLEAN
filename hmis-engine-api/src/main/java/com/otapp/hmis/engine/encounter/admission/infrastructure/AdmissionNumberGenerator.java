package com.otapp.hmis.engine.encounter.admission.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates {@code AD-YYYY-NNNNNN} admission numbers off a Postgres sequence
 * so concurrent admissions cannot collide.
 */
@Component
@RequiredArgsConstructor
public class AdmissionNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('admission_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("AD-%04d-%06d", year, seq);
    }
}
