package com.otapp.hmis.engine.billing.payment.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Generates {@code PMT-YYYY-NNNNNN} payment numbers from a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class PaymentNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em.createNativeQuery("SELECT nextval('payment_no_seq')").getSingleResult();
        return String.format("PMT-%04d-%06d", LocalDate.now().getYear(), raw.longValue());
    }
}
