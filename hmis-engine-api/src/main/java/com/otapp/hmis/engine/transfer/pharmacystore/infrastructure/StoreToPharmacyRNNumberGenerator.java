package com.otapp.hmis.engine.transfer.pharmacystore.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** {@code S2P-RN-YYYY-NNNNNN} numbers off a Postgres sequence. */
@Component
@RequiredArgsConstructor
public class StoreToPharmacyRNNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('store_to_pharmacy_rn_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("S2P-RN-%04d-%06d", year, seq);
    }
}
