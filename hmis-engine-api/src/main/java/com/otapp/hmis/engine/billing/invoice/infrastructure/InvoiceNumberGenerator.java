package com.otapp.hmis.engine.billing.invoice.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Generates {@code INV-YYYY-NNNNNN} invoice numbers from a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em.createNativeQuery("SELECT nextval('invoice_no_seq')").getSingleResult();
        return String.format("INV-%04d-%06d", LocalDate.now().getYear(), raw.longValue());
    }
}
