package com.otapp.hmis.engine.encounter.progressnote.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.encounter.progressnote.application.ProgressNoteDtos.CreateProgressNoteRequest;
import com.otapp.hmis.engine.encounter.progressnote.application.ProgressNoteDtos.DeleteProgressNoteRequest;
import com.otapp.hmis.engine.encounter.progressnote.application.ProgressNoteDtos.ProgressNoteDto;
import com.otapp.hmis.engine.encounter.progressnote.domain.ProgressNote;
import com.otapp.hmis.engine.encounter.progressnote.domain.ProgressNoteRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressNoteService {

    private final ProgressNoteRepository noteRepository;
    private final AdmissionRepository admissionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProgressNoteDto add(String admissionUid, CreateProgressNoteRequest request) {
        Admission admission = admissionRepository.findByUid(admissionUid)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + admissionUid));
        if (admission.getStatus() == AdmissionStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot add notes to a cancelled admission");
        }
        String username = currentUsername();
        if (username == null) {
            throw new BusinessRuleException("Authenticated user required to record a progress note");
        }
        ProgressNote note = new ProgressNote(admission.getUid(), request.kind(), username, request.body().trim());
        noteRepository.save(note);
        return toDto(note);
    }

    @Transactional
    public ProgressNoteDto softDelete(String uid, DeleteProgressNoteRequest request) {
        ProgressNote note = noteRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Progress note not found: " + uid));
        note.softDelete(currentUsername(), emptyToNull(request == null ? null : request.reason()));
        return toDto(note);
    }

    @Transactional(readOnly = true)
    public List<ProgressNoteDto> listForAdmission(String admissionUid) {
        return noteRepository.findByAdmissionUidOrderByRecordedAtDesc(admissionUid).stream()
                .map(this::toDto)
                .toList();
    }

    private ProgressNoteDto toDto(ProgressNote n) {
        User author = userRepository.findByUsername(n.getAuthorUsername()).orElse(null);
        String fullName = author == null ? null : (author.getFirstName() + " " + author.getLastName()).trim();
        return new ProgressNoteDto(
                n.getUid(),
                n.getAdmissionUid(),
                n.getKind(),
                n.getAuthorUsername(),
                fullName,
                n.getRecordedAt(),
                n.getBody(),
                n.isDeleted(),
                n.getDeletedAt(),
                n.getDeletedBy(),
                n.getDeletedReason());
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
