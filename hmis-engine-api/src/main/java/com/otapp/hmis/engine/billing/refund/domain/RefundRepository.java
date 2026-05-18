package com.otapp.hmis.engine.billing.refund.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByUid(String uid);

    List<Refund> findByInvoiceUidOrderByRefundedAtDesc(String invoiceUid);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.refundedAt >= :from AND r.refundedAt < :to")
    BigDecimal sumRefundedInRange(@Param("from") Instant from, @Param("to") Instant to);
}
