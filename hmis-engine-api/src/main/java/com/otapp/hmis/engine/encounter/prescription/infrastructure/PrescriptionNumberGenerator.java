package com.otapp.hmis.engine.encounter.prescription.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PrescriptionNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('prescription_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("RX-%04d-%06d", year, seq);
    }
}
