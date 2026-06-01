package com.otapp.hmis.engine.encounter.consultation.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationTransferRepository extends JpaRepository<ConsultationTransfer, Long> {

    Optional<ConsultationTransfer> findByUid(String uid);

    /**
     * Pending-transfer guard: a patient may have at most one PENDING transfer at a
     * time. Derived method (no JPQL enum-null pitfall).
     */
    boolean existsByPatientUidAndStatus(String patientUid, ConsultationTransferStatus status);

    /**
     * The receiving worklist — transfers in a given status, newest first. Derived
     * method so the status param is never compared against NULL in JPQL (the
     * Hibernate 6 {@code :p IS NULL} pitfall).
     */
    Page<ConsultationTransfer> findByStatusOrderByCreatedAtDesc(ConsultationTransferStatus status, Pageable pageable);
}
