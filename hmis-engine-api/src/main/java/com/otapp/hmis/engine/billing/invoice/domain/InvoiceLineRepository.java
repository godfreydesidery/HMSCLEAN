package com.otapp.hmis.engine.billing.invoice.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    List<InvoiceLine> findAllByInvoiceUidOrderByCreatedAtAsc(String invoiceUid);

    void deleteAllByInvoiceUid(String invoiceUid);

    /**
     * Returns the order/Rx uids ({@code reference_uid}) already billed on an
     * invoice for this patient — outsider regeneration uses this to avoid
     * double-billing items that landed on a prior (issued) invoice.
     */
    @Query("""
            SELECT DISTINCT l.referenceUid
            FROM Invoice i, InvoiceLine l
            WHERE l.invoiceUid = i.uid
              AND i.patientUid = :patientUid
              AND l.referenceUid IS NOT NULL
            """)
    List<String> findReferenceUidsBilledForPatient(@Param("patientUid") String patientUid);
}
