package com.otapp.hmis.engine.billing.refund.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** {@code RFD-YYYY-NNNNNN} refund numbers from a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class RefundNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em.createNativeQuery("SELECT nextval('refund_no_seq')").getSingleResult();
        return String.format("RFD-%04d-%06d", LocalDate.now().getYear(), raw.longValue());
    }
}
