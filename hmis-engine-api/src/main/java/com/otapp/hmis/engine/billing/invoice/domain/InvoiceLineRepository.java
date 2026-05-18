package com.otapp.hmis.engine.billing.invoice.domain;

import java.time.Instant;
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

    /**
     * Group revenue by service-kind for invoices issued in the given
     * range. Excludes DRAFT and CANCELLED invoices — only revenue that
     * actually hit the books counts.
     */
    @Query("""
            SELECT l.kind, SUM(l.amount)
            FROM Invoice i, InvoiceLine l
            WHERE l.invoiceUid = i.uid
              AND i.issuedAt IS NOT NULL
              AND i.issuedAt >= :from
              AND i.issuedAt <  :to
              AND i.status NOT IN ('DRAFT', 'CANCELLED')
            GROUP BY l.kind
            ORDER BY l.kind
            """)
    List<Object[]> sumAmountByKindInIssuedRange(@Param("from") Instant from,
                                                @Param("to") Instant to);

    /** Sum of all line amounts for invoices issued in range (excludes DRAFT / CANCELLED). */
    @Query("""
            SELECT COALESCE(SUM(l.amount), 0)
            FROM Invoice i, InvoiceLine l
            WHERE l.invoiceUid = i.uid
              AND i.issuedAt IS NOT NULL
              AND i.issuedAt >= :from
              AND i.issuedAt <  :to
              AND i.status NOT IN ('DRAFT', 'CANCELLED')
            """)
    java.math.BigDecimal sumTotalBilledInIssuedRange(@Param("from") Instant from,
                                                     @Param("to") Instant to);
}
