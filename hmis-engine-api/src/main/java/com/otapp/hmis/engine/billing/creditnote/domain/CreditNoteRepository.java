package com.otapp.hmis.engine.billing.creditnote.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    Optional<CreditNote> findByUid(String uid);

    List<CreditNote> findByInvoiceUidOrderByIssuedAtDesc(String invoiceUid);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CreditNote c WHERE c.issuedAt >= :from AND c.issuedAt < :to")
    BigDecimal sumCreditedInRange(@Param("from") Instant from, @Param("to") Instant to);
}
