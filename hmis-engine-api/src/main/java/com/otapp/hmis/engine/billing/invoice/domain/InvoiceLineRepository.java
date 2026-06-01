package com.otapp.hmis.engine.billing.invoice.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    Optional<InvoiceLine> findByUid(String uid);

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

    /**
     * Unclaimed COVERED lines routed to a payer plan + member, on invoices that
     * are on the books (not DRAFT / CANCELLED) — backs insurance-claim assembly.
     * Oldest first so a claim reads in charge order.
     */
    @Query("""
            SELECT l
            FROM Invoice i, InvoiceLine l
            WHERE l.invoiceUid = i.uid
              AND l.coverageStatus = 'COVERED'
              AND l.payerPlanUid = :payerPlanUid
              AND l.membershipNo = :membershipNo
              AND l.claimId IS NULL
              AND i.status NOT IN ('DRAFT', 'CANCELLED')
            ORDER BY l.createdAt ASC
            """)
    List<InvoiceLine> findClaimableCoveredLines(@Param("payerPlanUid") String payerPlanUid,
                                                @Param("membershipNo") String membershipNo);

    /**
     * A patient's cash-payable lines across every open <em>outpatient</em> invoice —
     * the cashier "check to pay" queue (legacy {@code get_*_bills} returning UNPAID
     * {@code PatientBill}s). A line qualifies when its invoice is on the books
     * (not DRAFT / CANCELLED) and still owes money — filtered on the computed
     * balance, not status, because adding a charge line to an already-PAID invoice
     * raises its balance without rolling the status back — and the line itself is
     * cash-routed (UNPAID / VERIFIED) with cash still outstanding. COVERED lines
     * (insurer money) are excluded. ADMISSION invoices are excluded too: inpatient
     * charges (ward-day accrual + VERIFIED accruals) settle through the admission's
     * own deposit / discharge billing, and collecting one here would freeze the
     * still-accruing ward bill — so they must not bleed into the outpatient till.
     * Oldest charge first.
     */
    @Query("""
            SELECT l
            FROM Invoice i, InvoiceLine l
            WHERE l.invoiceUid = i.uid
              AND i.patientUid = :patientUid
              AND i.status NOT IN ('DRAFT', 'CANCELLED')
              AND i.scope <> 'ADMISSION'
              AND (i.subtotal - i.totalPaid - i.totalCredited - i.totalCovered) > 0
              AND l.coverageStatus IN ('UNPAID', 'VERIFIED')
              AND l.amount > l.paidAmount
            ORDER BY l.createdAt ASC
            """)
    List<InvoiceLine> findPayableLinesForPatient(@Param("patientUid") String patientUid);

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

    /**
     * Pharmacy sales (BILL-5): MEDICINE lines on invoices issued in range
     * (excludes DRAFT / CANCELLED), grouped by medicine. A MEDICINE line stores
     * the medicine uid in {@code serviceUid}. Rows are
     * {@code [String medicineUid, BigDecimal quantity, BigDecimal amount, Long lineCount]}.
     */
    @Query("""
            SELECT l.serviceUid, COALESCE(SUM(l.quantity), 0), COALESCE(SUM(l.amount), 0), COUNT(l)
            FROM Invoice i, InvoiceLine l
            WHERE l.invoiceUid = i.uid
              AND l.kind = 'MEDICINE'
              AND i.issuedAt IS NOT NULL
              AND i.issuedAt >= :from
              AND i.issuedAt <  :to
              AND i.status NOT IN ('DRAFT', 'CANCELLED')
            GROUP BY l.serviceUid
            """)
    List<Object[]> pharmacySalesInRange(@Param("from") Instant from, @Param("to") Instant to);
}
