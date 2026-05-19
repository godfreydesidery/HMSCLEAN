package com.otapp.hmis.engine.encounter.labbatch.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates {@code LBT-YYYY-NNNNNN} lab-batch numbers off a Postgres
 * sequence so concurrent batch creation cannot collide.
 */
@Component
@RequiredArgsConstructor
public class LabBatchNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('lab_batch_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("LBT-%04d-%06d", year, seq);
    }
}
