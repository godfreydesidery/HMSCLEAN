package com.otapp.hmis.engine.encounter.vitals.api;

import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.PatientVitalsDto;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.RecordVitalsRequest;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Outpatient vitals capture (OPC-3) — the two-actor lifecycle
 * EMPTY → PENDING → SUBMITTED → ARCHIVED, mounted under the consultation it
 * belongs to. The outpatient nurse-triage worklist lives on
 * {@code OutpatientNurseWorklistController}, mounted at
 * {@code /encounters/consultations/nurse-worklist} (collection-level, not uid-scoped).
 */
@Tag(name = "Patient vitals")
@RestController
@RequestMapping("/encounters/consultations/uid/{consultationUid}/vitals")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class PatientVitalsController {

    private final PatientVitalsService vitalsService;

    @GetMapping
    public ResponseEntity<List<PatientVitalsDto>> list(@PathVariable String consultationUid) {
        return ResponseEntity.ok(vitalsService.listForConsultation(consultationUid));
    }

    /**
     * Nurse fill / save: create-or-update the open vitals row with the readings,
     * leaving it PENDING (editable until submitted).
     */
    @Operation(summary = "Nurse fill/save vitals (→ PENDING)")
    @PostMapping
    public ResponseEntity<PatientVitalsDto> save(@PathVariable String consultationUid,
                                                 @Valid @RequestBody RecordVitalsRequest request) {
        return ResponseEntity.ok(vitalsService.save(consultationUid, request));
    }

    /** Nurse submit: PENDING → SUBMITTED. The set is then locked. */
    @Operation(summary = "Nurse submit vitals (PENDING → SUBMITTED, locks the set)")
    @PostMapping("/uid/{vitalsUid}/submit")
    public ResponseEntity<PatientVitalsDto> submit(@PathVariable String consultationUid,
                                                   @PathVariable String vitalsUid) {
        return ResponseEntity.ok(vitalsService.submit(vitalsUid));
    }

    /** Doctor consume: SUBMITTED → ARCHIVED — taken into the clinical exam. */
    @Operation(summary = "Doctor consume vitals (SUBMITTED → ARCHIVED)")
    @PostMapping("/uid/{vitalsUid}/consume")
    public ResponseEntity<PatientVitalsDto> consume(@PathVariable String consultationUid,
                                                    @PathVariable String vitalsUid) {
        return ResponseEntity.ok(vitalsService.consume(vitalsUid));
    }

    @DeleteMapping("/uid/{vitalsUid}")
    public ResponseEntity<Void> delete(@PathVariable String consultationUid, @PathVariable String vitalsUid) {
        vitalsService.delete(vitalsUid);
        return ResponseEntity.noContent().build();
    }
}
