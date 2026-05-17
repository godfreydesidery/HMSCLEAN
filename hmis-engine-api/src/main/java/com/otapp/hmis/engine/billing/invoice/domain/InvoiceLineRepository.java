package com.otapp.hmis.engine.billing.invoice.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    List<InvoiceLine> findAllByInvoiceUidOrderByCreatedAtAsc(String invoiceUid);

    void deleteAllByInvoiceUid(String invoiceUid);
}
