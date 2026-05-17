package com.otapp.hmis.engine.patient.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates human-readable patient numbers in the form {@code PT-YYYY-NNNNNN}
 * using a Postgres sequence so concurrent registrations don't collide.
 */
@Component
@RequiredArgsConstructor
public class PatientNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('patient_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("PT-%04d-%06d", year, seq);
    }
}
