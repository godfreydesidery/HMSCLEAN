package com.otapp.hmis.engine.encounter.order.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates {@code CLO-YYYY-NNNNNN} clinical order numbers off a dedicated
 * Postgres sequence.
 */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('clinical_order_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("CLO-%04d-%06d", year, seq);
    }
}
