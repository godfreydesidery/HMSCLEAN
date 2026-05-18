package com.otapp.hmis.engine.transfer.pharmacypharmacy.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** {@code P2P-RN-YYYY-NNNNNN} numbers off a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class PharmacyToPharmacyRNNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('pharmacy_to_pharmacy_rn_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("P2P-RN-%04d-%06d", year, seq);
    }
}
