package com.otapp.hmis.engine.encounter.vitals.api;

import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.PatientVitalsDto;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsDtos.RecordVitalsRequest;
import com.otapp.hmis.engine.encounter.vitals.application.PatientVitalsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<PatientVitalsDto> record(@PathVariable String consultationUid,
                                                   @Valid @RequestBody RecordVitalsRequest request) {
        return ResponseEntity.ok(vitalsService.record(consultationUid, request));
    }

    @DeleteMapping("/uid/{vitalsUid}")
    public ResponseEntity<Void> delete(@PathVariable String consultationUid, @PathVariable String vitalsUid) {
        vitalsService.delete(vitalsUid);
        return ResponseEntity.noContent().build();
    }
}
