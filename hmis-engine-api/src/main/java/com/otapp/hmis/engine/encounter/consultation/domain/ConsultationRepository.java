package com.otapp.hmis.engine.encounter.consultation.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByUid(String uid);

    List<Consultation> findTop10ByPatientUidOrderByBookedAtDesc(String patientUid);

    /**
     * Active-encounter guard (legacy do_consultation / change_type / change_payment_type):
     * count the patient's consultations in any "ongoing" status. Used to block a
     * second booking ("wait for the patient to be released") and to block a
     * patient type / payment-type change while an encounter is in progress.
     */
    long countByPatientUidAndStatusIn(String patientUid, Collection<ConsultationStatus> statuses);

    @Query("""
            SELECT COUNT(c) FROM Consultation c
            WHERE c.clinicianUsername = :clinician
              AND c.bookedAt >= :from
              AND c.bookedAt <  :to
            """)
    long countByClinicianInRange(@Param("clinician") String clinicianUsername,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);

    @Query("""
            SELECT c FROM Consultation c
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(c.consultationNo)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.patientUid)        LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.clinicianUsername) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status     IS NULL OR c.status            = :status)
              AND (:clinicUid  IS NULL OR c.clinicUid         = :clinicUid)
              AND (:patientUid IS NULL OR c.patientUid        = :patientUid)
              AND (:clinician  IS NULL OR c.clinicianUsername = :clinician)
            """)
    Page<Consultation> search(@Param("search")     String search,
                              @Param("status")     ConsultationStatus status,
                              @Param("clinicUid")  String clinicUid,
                              @Param("patientUid") String patientUid,
                              @Param("clinician")  String clinicianUsername,
                              Pageable pageable);

    /**
     * The doctor's "from reception" queue: BOOKED consultations assigned to the
     * clinician whose fee is settled (CASH paid) or whose payment type is not
     * CASH (legacy COVERED). Oldest first — first-come, first-served.
     */
    @Query("""
            SELECT c FROM Consultation c
            WHERE c.clinicianUsername = :clinician
              AND c.status = com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus.BOOKED
              AND (c.feeSettled = true
                   OR c.paymentType <> com.otapp.hmis.engine.patient.domain.PaymentType.CASH)
            ORDER BY c.bookedAt ASC
            """)
    Page<Consultation> findReceptionQueueFor(@Param("clinician") String clinicianUsername, Pageable pageable);

    /**
     * The OUTPATIENT nurse-triage worklist (OPC-3): fee-settled consultations
     * still BOOKED or IN_PROGRESS — the patients who may need vitals captured.
     * Payment-gated on the denormalised {@code feeSettled} flag (legacy gated on
     * the consultation bill being PAID/COVERED/VERIFIED/NONE); the encounter
     * module never reads billing. Oldest first — first-come, first-served.
     * Status literals are pinned (no nullable enum param) to avoid the
     * Hibernate-6 {@code :p IS NULL} enum-binding trap.
     */
    @Query("""
            SELECT c FROM Consultation c
            WHERE c.feeSettled = true
              AND c.status IN (
                    com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus.BOOKED,
                    com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus.IN_PROGRESS)
            ORDER BY c.bookedAt ASC
            """)
    Page<Consultation> findOutpatientNurseWorklist(Pageable pageable);
}
