package com.otapp.hmis.engine.procurement.receipt.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates {@code GR-YYYY-NNNNNN} goods-receipt numbers off a Postgres
 * sequence.
 */
@Component
@RequiredArgsConstructor
public class GoodsReceiptNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('goods_receipt_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("GR-%04d-%06d", year, seq);
    }
}
