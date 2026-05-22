package com.otapp.hmis.engine.procurement.supplierinvoice.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierInvoiceLineRepository extends JpaRepository<SupplierInvoiceLine, Long> {

    Optional<SupplierInvoiceLine> findByUid(String uid);

    List<SupplierInvoiceLine> findAllByInvoiceUidOrderByCreatedAtAsc(String invoiceUid);

    void deleteAllByInvoiceUid(String invoiceUid);
}
