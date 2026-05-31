package com.otapp.hmis.engine.billing.claim.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** {@code CLM-YYYY-NNNNNN} claim numbers from a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class ClaimNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em.createNativeQuery("SELECT nextval('insurance_claim_no_seq')").getSingleResult();
        return String.format("CLM-%04d-%06d", LocalDate.now().getYear(), raw.longValue());
    }
}
