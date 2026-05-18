package com.otapp.hmis.engine.billing.refund.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByUid(String uid);

    List<Refund> findByInvoiceUidOrderByRefundedAtDesc(String invoiceUid);
}
