package com.otapp.hmis.engine.encounter.admission.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.AdmissionDto;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.AdmissionSummary;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.AdmitPatientRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.CancelAdmissionRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.DischargeRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionDtos.TransferWardRequest;
import com.otapp.hmis.engine.encounter.admission.application.AdmissionService;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
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

@Tag(name = "Admissions")
@RestController
@RequestMapping("/encounters/admissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class AdmissionController {

    private final AdmissionService admissionService;

    @PostMapping
    public ResponseEntity<AdmissionDto> admit(@Valid @RequestBody AdmitPatientRequest request) {
        AdmissionDto created = admissionService.admit(request);
        URI loc = UriComponentsBuilder.fromPath("/encounters/admissions/{uid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<AdmissionSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) AdmissionStatus status,
            @RequestParam(required = false) String wardUid,
            @RequestParam(required = false) String patientUid,
            Pageable pageable) {
        return ResponseEntity.ok(admissionService.search(query, status, wardUid, patientUid, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<AdmissionDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(admissionService.findByUid(uid));
    }

    @PostMapping("/{uid}/transfer")
    public ResponseEntity<AdmissionDto> transferWard(@PathVariable String uid,
                                                     @Valid @RequestBody TransferWardRequest request) {
        return ResponseEntity.ok(admissionService.transferWard(uid, request));
    }

    @PostMapping("/{uid}/discharge")
    public ResponseEntity<AdmissionDto> discharge(@PathVariable String uid,
                                                  @Valid @RequestBody(required = false) DischargeRequest request) {
        return ResponseEntity.ok(admissionService.discharge(uid, request));
    }

    @PostMapping("/{uid}/deceased")
    public ResponseEntity<AdmissionDto> markDeceased(@PathVariable String uid,
                                                     @Valid @RequestBody(required = false) DischargeRequest request) {
        return ResponseEntity.ok(admissionService.markDeceased(uid, request));
    }

    @PostMapping("/{uid}/transfer-out")
    public ResponseEntity<AdmissionDto> transferOut(@PathVariable String uid,
                                                    @Valid @RequestBody(required = false) DischargeRequest request) {
        return ResponseEntity.ok(admissionService.transferOut(uid, request));
    }

    @PostMapping("/{uid}/cancel")
    public ResponseEntity<AdmissionDto> cancel(@PathVariable String uid,
                                               @Valid @RequestBody(required = false) CancelAdmissionRequest request) {
        return ResponseEntity.ok(admissionService.cancel(uid, request));
    }

    @GetMapping("/by-patient/{patientUid}/recent")
    public ResponseEntity<List<AdmissionSummary>> recentForPatient(@PathVariable String patientUid) {
        return ResponseEntity.ok(admissionService.recentForPatient(patientUid));
    }
}
