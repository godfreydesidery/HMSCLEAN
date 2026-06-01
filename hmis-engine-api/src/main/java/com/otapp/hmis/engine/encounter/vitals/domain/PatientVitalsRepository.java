package com.otapp.hmis.engine.encounter.vitals.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientVitalsRepository extends JpaRepository<PatientVitals, Long> {

    Optional<PatientVitals> findByUid(String uid);

    List<PatientVitals> findAllByConsultationUidOrderByTakenAtDesc(String consultationUid);

    /**
     * The single "open" (still-fillable) vitals row for a consultation, if any —
     * an EMPTY or PENDING row the nurse may still save into. Newest first so a
     * re-materialised row wins. Statuses are passed as {@code .name()} strings to
     * a {@code String}-bound query elsewhere; here a derived method keeps it type-safe.
     */
    Optional<PatientVitals> findFirstByConsultationUidAndStatusInOrderByTakenAtDesc(
            String consultationUid, java.util.Collection<VitalsStatus> statuses);

    /** The most recent vitals row for a consultation, regardless of status — drives the worklist's status badge. */
    Optional<PatientVitals> findFirstByConsultationUidOrderByTakenAtDesc(String consultationUid);
}
