package com.otapp.hmis.engine.encounter.note.application;

import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.consultation.domain.Consultation;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationRepository;
import com.otapp.hmis.engine.encounter.note.application.ClinicalNoteDtos.ClinicalNoteDto;
import com.otapp.hmis.engine.encounter.note.application.ClinicalNoteDtos.SaveClinicalNoteRequest;
import com.otapp.hmis.engine.encounter.note.domain.ClinicalNote;
import com.otapp.hmis.engine.encounter.note.domain.ClinicalNoteRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicalNoteService {

    private final ClinicalNoteRepository noteRepository;
    private final ConsultationRepository consultationRepository;

    @Transactional(readOnly = true)
    public Optional<ClinicalNoteDto> findForConsultation(String consultationUid) {
        return noteRepository.findByConsultationUid(consultationUid).map(ClinicalNoteService::toDto);
    }

    /**
     * Upsert — creates the note row on first save, edits it thereafter.
     */
    @Transactional
    public ClinicalNoteDto save(String consultationUid, SaveClinicalNoteRequest request) {
        Consultation consultation = consultationRepository.findByUid(consultationUid)
                .orElseThrow(() -> new NotFoundException("Consultation not found: " + consultationUid));
        // Legacy open_consultation confinement: clinical entries only while IN_PROGRESS.
        consultation.requireAuthorable();
        ClinicalNote note = noteRepository.findByConsultationUid(consultationUid)
                .orElseGet(() -> new ClinicalNote(consultationUid));

        note.setChiefComplaint(emptyToNull(request.chiefComplaint()));
        note.setHistoryOfPresentingIllness(emptyToNull(request.historyOfPresentingIllness()));
        note.setPastMedicalHistory(emptyToNull(request.pastMedicalHistory()));
        note.setExamination(emptyToNull(request.examination()));
        note.setAssessment(emptyToNull(request.assessment()));
        note.setPlan(emptyToNull(request.plan()));
        note.setDrugsAndAllergyHistory(emptyToNull(request.drugsAndAllergyHistory()));
        note.setFamilyAndSocialHistory(emptyToNull(request.familyAndSocialHistory()));
        note.setReviewOfOtherSystems(emptyToNull(request.reviewOfOtherSystems()));
        noteRepository.save(note);

        return toDto(note);
    }

    private static ClinicalNoteDto toDto(ClinicalNote n) {
        return new ClinicalNoteDto(
                n.getId(),
                n.getUid(),
                n.getConsultationUid(),
                n.getChiefComplaint(),
                n.getHistoryOfPresentingIllness(),
                n.getPastMedicalHistory(),
                n.getExamination(),
                n.getAssessment(),
                n.getPlan(),
                n.getDrugsAndAllergyHistory(),
                n.getFamilyAndSocialHistory(),
                n.getReviewOfOtherSystems(),
                n.getCreatedAt(),
                n.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
