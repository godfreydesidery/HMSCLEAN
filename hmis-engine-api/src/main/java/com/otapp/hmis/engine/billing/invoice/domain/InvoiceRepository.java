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
