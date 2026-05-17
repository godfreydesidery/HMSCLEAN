package com.otapp.hmis.engine.encounter.consultation.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.CancelConsultationRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationDto;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationSummary;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.StartConsultationRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationService;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Consultations")
@RestController
@RequestMapping("/encounters/consultations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    public ResponseEntity<ConsultationDto> book(@Valid @RequestBody StartConsultationRequest request) {
        ConsultationDto created = consultationService.book(request);
        URI loc = UriComponentsBuilder.fromPath("/encounters/consultations/{uid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ConsultationSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ConsultationStatus status,
            @RequestParam(required = false) String clinicUid,
            @RequestParam(required = false) String patientUid,
            @RequestParam(required = false) String clinicianUsername,
            Pageable pageable) {
        return ResponseEntity.ok(consultationService.search(query, status, clinicUid, patientUid, clinicianUsername, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<ConsultationDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(consultationService.findByUid(uid));
    }

    @PostMapping("/{uid}/start")
    public ResponseEntity<ConsultationDto> start(@PathVariable String uid) {
        return ResponseEntity.ok(consultationService.start(uid));
    }

    @PostMapping("/{uid}/complete")
    public ResponseEntity<ConsultationDto> complete(@PathVariable String uid) {
        return ResponseEntity.ok(consultationService.complete(uid));
    }

    @PostMapping("/{uid}/cancel")
    public ResponseEntity<ConsultationDto> cancel(@PathVariable String uid,
                                                  @Valid @RequestBody(required = false) CancelConsultationRequest request) {
        return ResponseEntity.ok(consultationService.cancel(uid, request));
    }

    @GetMapping("/by-patient/{patientUid}/recent")
    public ResponseEntity<List<ConsultationSummary>> recentForPatient(@PathVariable String patientUid) {
        return ResponseEntity.ok(consultationService.recentForPatient(patientUid));
    }
}
