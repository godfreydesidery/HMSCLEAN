package com.otapp.hmis.engine.encounter.note.api;

import com.otapp.hmis.engine.encounter.note.application.ClinicalNoteDtos.ClinicalNoteDto;
import com.otapp.hmis.engine.encounter.note.application.ClinicalNoteDtos.SaveClinicalNoteRequest;
import com.otapp.hmis.engine.encounter.note.application.ClinicalNoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Clinical notes")
@RestController
@RequestMapping("/encounters/consultations/uid/{consultationUid}/clinical-note")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ClinicalNoteController {

    private final ClinicalNoteService noteService;

    /** Returns 200 with the note, or 204 if none has been recorded yet. */
    @GetMapping
    public ResponseEntity<ClinicalNoteDto> get(@PathVariable String consultationUid) {
        return noteService.findForConsultation(consultationUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<ClinicalNoteDto> save(@PathVariable String consultationUid,
                                                @Valid @RequestBody SaveClinicalNoteRequest request) {
        return ResponseEntity.ok(noteService.save(consultationUid, request));
    }
}
