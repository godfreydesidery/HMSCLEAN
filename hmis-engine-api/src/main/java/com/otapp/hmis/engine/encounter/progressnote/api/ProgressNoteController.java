package com.otapp.hmis.engine.encounter.progressnote.api;

import com.otapp.hmis.engine.encounter.progressnote.application.ProgressNoteDtos.CreateProgressNoteRequest;
import com.otapp.hmis.engine.encounter.progressnote.application.ProgressNoteDtos.DeleteProgressNoteRequest;
import com.otapp.hmis.engine.encounter.progressnote.application.ProgressNoteDtos.ProgressNoteDto;
import com.otapp.hmis.engine.encounter.progressnote.application.ProgressNoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admission progress notes")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ProgressNoteController {

    private final ProgressNoteService noteService;

    @GetMapping("/encounters/admissions/uid/{admissionUid}/progress-notes")
    public ResponseEntity<List<ProgressNoteDto>> list(@PathVariable String admissionUid) {
        return ResponseEntity.ok(noteService.listForAdmission(admissionUid));
    }

    @PostMapping("/encounters/admissions/uid/{admissionUid}/progress-notes")
    public ResponseEntity<ProgressNoteDto> add(@PathVariable String admissionUid,
                                               @Valid @RequestBody CreateProgressNoteRequest request) {
        return ResponseEntity.ok(noteService.add(admissionUid, request));
    }

    @DeleteMapping("/encounters/progress-notes/uid/{progressNoteUid}")
    public ResponseEntity<ProgressNoteDto> softDelete(@PathVariable String progressNoteUid,
                                                      @Valid @RequestBody(required = false) DeleteProgressNoteRequest request) {
        return ResponseEntity.ok(noteService.softDelete(progressNoteUid, request));
    }
}
