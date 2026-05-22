package com.otapp.hmis.engine.transfer.pharmacypharmacy.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** {@code P2P-RO-YYYY-NNNNNN} numbers off a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class PharmacyToPharmacyRONumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('pharmacy_to_pharmacy_ro_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("P2P-RO-%04d-%06d", year, seq);
    }
}
