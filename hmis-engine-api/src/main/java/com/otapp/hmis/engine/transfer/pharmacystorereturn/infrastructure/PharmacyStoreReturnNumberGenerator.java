package com.otapp.hmis.engine.transfer.pharmacystorereturn.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** {@code P2S-RT-YYYY-NNNNNN} return numbers off a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class PharmacyStoreReturnNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('pharmacy_store_return_no_seq')")
                .getSingleResult();
        return String.format("P2S-RT-%04d-%06d", LocalDate.now().getYear(), raw.longValue());
    }
}
