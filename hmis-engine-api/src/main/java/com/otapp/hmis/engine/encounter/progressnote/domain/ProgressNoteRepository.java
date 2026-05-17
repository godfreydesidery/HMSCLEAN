package com.otapp.hmis.engine.encounter.progressnote.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressNoteRepository extends JpaRepository<ProgressNote, Long> {

    Optional<ProgressNote> findByUid(String uid);

    List<ProgressNote> findByAdmissionUidOrderByRecordedAtDesc(String admissionUid);
}
