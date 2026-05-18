package com.otapp.hmis.engine.billing.creditnote.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    Optional<CreditNote> findByUid(String uid);

    List<CreditNote> findByInvoiceUidOrderByIssuedAtDesc(String invoiceUid);
}
