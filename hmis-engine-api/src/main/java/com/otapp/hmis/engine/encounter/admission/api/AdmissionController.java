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
        URI loc = UriComponentsBuilder.fromPath("/encounters/admissions/uid/{admissionUid}")
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

    @GetMapping("/uid/{admissionUid}")
    public ResponseEntity<AdmissionDto> findByUid(@PathVariable String admissionUid) {
        return ResponseEntity.ok(admissionService.findByUid(admissionUid));
    }

    @PostMapping("/uid/{admissionUid}/transfer")
    public ResponseEntity<AdmissionDto> transferWard(@PathVariable String admissionUid,
                                                     @Valid @RequestBody TransferWardRequest request) {
        return ResponseEntity.ok(admissionService.transferWard(admissionUid, request));
    }

    @PostMapping("/uid/{admissionUid}/discharge")
    public ResponseEntity<AdmissionDto> discharge(@PathVariable String admissionUid,
                                                  @Valid @RequestBody(required = false) DischargeRequest request) {
        return ResponseEntity.ok(admissionService.discharge(admissionUid, request));
    }

    @PostMapping("/uid/{admissionUid}/deceased")
    public ResponseEntity<AdmissionDto> markDeceased(@PathVariable String admissionUid,
                                                     @Valid @RequestBody(required = false) DischargeRequest request) {
        return ResponseEntity.ok(admissionService.markDeceased(admissionUid, request));
    }

    @PostMapping("/uid/{admissionUid}/transfer-out")
    public ResponseEntity<AdmissionDto> transferOut(@PathVariable String admissionUid,
                                                    @Valid @RequestBody(required = false) DischargeRequest request) {
        return ResponseEntity.ok(admissionService.transferOut(admissionUid, request));
    }

    @PostMapping("/uid/{admissionUid}/cancel")
    public ResponseEntity<AdmissionDto> cancel(@PathVariable String admissionUid,
                                               @Valid @RequestBody(required = false) CancelAdmissionRequest request) {
        return ResponseEntity.ok(admissionService.cancel(admissionUid, request));
    }

    @GetMapping("/by-patient/uid/{patientUid}/recent")
    public ResponseEntity<List<AdmissionSummary>> recentForPatient(@PathVariable String patientUid) {
        return ResponseEntity.ok(admissionService.recentForPatient(patientUid));
    }
}
