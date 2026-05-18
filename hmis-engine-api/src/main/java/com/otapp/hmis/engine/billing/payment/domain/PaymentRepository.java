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
}
