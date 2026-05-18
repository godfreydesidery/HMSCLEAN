package com.otapp.hmis.engine.encounter.diagnosis.api;

import com.otapp.hmis.engine.encounter.diagnosis.application.ConsultationDiagnosisDtos.AddDiagnosisRequest;
import com.otapp.hmis.engine.encounter.diagnosis.application.ConsultationDiagnosisDtos.ConsultationDiagnosisDto;
import com.otapp.hmis.engine.encounter.diagnosis.application.ConsultationDiagnosisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Consultation diagnoses")
@RestController
@RequestMapping("/encounters/consultations/uid/{consultationUid}/diagnoses")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ConsultationDiagnosisController {

    private final ConsultationDiagnosisService service;

    @GetMapping
    public ResponseEntity<List<ConsultationDiagnosisDto>> list(@PathVariable String consultationUid) {
        return ResponseEntity.ok(service.listForConsultation(consultationUid));
    }

    @PostMapping
    public ResponseEntity<ConsultationDiagnosisDto> add(@PathVariable String consultationUid,
                                                       @Valid @RequestBody AddDiagnosisRequest request) {
        return ResponseEntity.ok(service.add(consultationUid, request));
    }

    @DeleteMapping("/uid/{diagnosisUid}")
    public ResponseEntity<Void> remove(@PathVariable String consultationUid, @PathVariable String diagnosisUid) {
        service.remove(diagnosisUid);
        return ResponseEntity.noContent().build();
    }
}
