package com.otapp.hmis.engine.billing.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUid(String uid);

    List<Payment> findAllByInvoiceUidOrderByReceivedAtAsc(String invoiceUid);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.receivedAt >= :from AND p.receivedAt < :to")
    BigDecimal sumReceivedInRange(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Sum of CASH payments captured by {@code createdBy} in
     * [{@code from}, {@code to}). Drives the cashier-shift expected-cash
     * calculation. Other methods (mobile, card, bank, insurance) don't
     * hit the till and are excluded.
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.method = com.otapp.hmis.engine.billing.payment.domain.PaymentMethod.CASH
              AND p.createdBy = :username
              AND p.receivedAt >= :from
              AND p.receivedAt <  :to
            """)
    BigDecimal sumCashByUserInRange(@Param("username") String username,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);

    /**
     * Revenue grouped by payment method over [{@code from}, {@code to}) — backs
     * the revenue-by-payment-mode report (BILL-5). Rows are
     * {@code [PaymentMethod method, BigDecimal amount, Long count]}.
     */
    @Query("""
            SELECT p.method, COALESCE(SUM(p.amount), 0), COUNT(p)
            FROM Payment p
            WHERE p.receivedAt >= :from AND p.receivedAt < :to
            GROUP BY p.method
            ORDER BY p.method
            """)
    List<Object[]> sumByMethodInRange(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Collections grouped by cashier and method over [{@code from}, {@code to}) —
     * backs the per-cashier cash-up / collections report (BILL-2). Rows are
     * {@code [String createdBy, PaymentMethod method, BigDecimal amount, Long count]}.
     */
    @Query("""
            SELECT p.createdBy, p.method, COALESCE(SUM(p.amount), 0), COUNT(p)
            FROM Payment p
            WHERE p.receivedAt >= :from AND p.receivedAt < :to
            GROUP BY p.createdBy, p.method
            ORDER BY p.createdBy
            """)
    List<Object[]> collectionsByUserAndMethodInRange(@Param("from") Instant from, @Param("to") Instant to);
}
