package com.otapp.hmis.engine.encounter.note.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    Optional<ClinicalNote> findByUid(String uid);

    Optional<ClinicalNote> findByConsultationUid(String consultationUid);
}
