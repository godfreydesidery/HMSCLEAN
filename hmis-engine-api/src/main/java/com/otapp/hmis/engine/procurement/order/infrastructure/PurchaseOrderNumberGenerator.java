package com.otapp.hmis.engine.procurement.order.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates {@code PO-YYYY-NNNNNN} purchase-order numbers off a Postgres
 * sequence so concurrent drafts cannot collide.
 */
@Component
@RequiredArgsConstructor
public class PurchaseOrderNumberGenerator {

    private final EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Number raw = (Number) em
                .createNativeQuery("SELECT nextval('purchase_order_no_seq')")
                .getSingleResult();
        long seq = raw.longValue();
        int year = LocalDate.now().getYear();
        return String.format("PO-%04d-%06d", year, seq);
    }
}
