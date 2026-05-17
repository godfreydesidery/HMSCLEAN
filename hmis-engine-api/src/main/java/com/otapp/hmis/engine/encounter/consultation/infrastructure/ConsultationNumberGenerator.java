package com.otapp.hmis.engine.encounter.consultation.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates {@code CN-YYYY-NNNNNN} consultation numbers off a Postgres
 * sequence so concurrent bookings cannot collide.
 */
@Component
@RequiredArgsConstructor
public class ConsultationNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('consultation_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("CN-%04d-%06d", year, seq);
    }
}
