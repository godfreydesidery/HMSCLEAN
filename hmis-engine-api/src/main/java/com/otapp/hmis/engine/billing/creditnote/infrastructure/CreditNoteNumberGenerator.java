package com.otapp.hmis.engine.billing.creditnote.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** {@code CN-YYYY-NNNNNN} credit-note numbers from a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class CreditNoteNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em.createNativeQuery("SELECT nextval('credit_note_no_seq')").getSingleResult();
        return String.format("CN-%04d-%06d", LocalDate.now().getYear(), raw.longValue());
    }
}
