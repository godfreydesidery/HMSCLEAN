package com.otapp.hmis.engine.billing.invoice.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByUid(String uid);

    Optional<Invoice> findByConsultationUid(String consultationUid);

    Optional<Invoice> findByAdmissionUid(String admissionUid);

    /**
     * The patient's currently-open OUTSIDER invoice (still DRAFT). Used to
     * decide whether to keep building lines on the existing invoice or start
     * a fresh one. Registration invoices are excluded via {@code scope}.
     */
    @Query("""
            SELECT i FROM Invoice i
            WHERE i.patientUid = :patientUid
              AND i.scope  = com.otapp.hmis.engine.billing.invoice.domain.InvoiceScope.OUTSIDER
              AND i.status = com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus.DRAFT
            """)
    Optional<Invoice> findDraftOutsiderForPatient(@Param("patientUid") String patientUid);

    /** All outsider invoices for a patient, newest first — for the patient detail view. */
    @Query("""
            SELECT i FROM Invoice i
            WHERE i.patientUid = :patientUid
              AND i.scope = com.otapp.hmis.engine.billing.invoice.domain.InvoiceScope.OUTSIDER
            ORDER BY i.createdAt DESC
            """)
    java.util.List<Invoice> findAllOutsiderForPatient(@Param("patientUid") String patientUid);

    /**
     * Looks up the registration invoice for a patient. There is at most one
     * (any status) — registration is a one-time fee per patient lifetime.
     */
    @Query("""
            SELECT i FROM Invoice i
            WHERE i.patientUid = :patientUid
              AND i.scope = com.otapp.hmis.engine.billing.invoice.domain.InvoiceScope.REGISTRATION
            """)
    Optional<Invoice> findRegistrationForPatient(@Param("patientUid") String patientUid);

    /**
     * True if the patient owes anything against their registration invoice
     * (issued / partially-paid with positive balance). Used to gate
     * consultation booking for CASH patients. A zero-balance invoice
     * (waived under an insurance plan, fully credited, etc.) does not block.
     */
    @Query("""
            SELECT COUNT(i) > 0 FROM Invoice i
            WHERE i.patientUid = :patientUid
              AND i.scope  = com.otapp.hmis.engine.billing.invoice.domain.InvoiceScope.REGISTRATION
              AND i.status IN (com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus.ISSUED,
                               com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus.PARTIALLY_PAID)
              AND (i.subtotal - i.totalPaid - i.totalCredited) > 0
            """)
    boolean hasUnpaidRegistration(@Param("patientUid") String patientUid);

    @Query("""
            SELECT i FROM Invoice i
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(i.invoiceNo)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.patientUid) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status     IS NULL OR i.status     = :status)
              AND (:patientUid IS NULL OR i.patientUid = :patientUid)
            """)
    Page<Invoice> search(@Param("search") String search,
                         @Param("status") InvoiceStatus status,
                         @Param("patientUid") String patientUid,
                         Pageable pageable);
}
