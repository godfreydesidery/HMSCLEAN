package com.otapp.hmis.engine.billing.payment.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUid(String uid);

    List<Payment> findAllByInvoiceUidOrderByReceivedAtAsc(String invoiceUid);
}
