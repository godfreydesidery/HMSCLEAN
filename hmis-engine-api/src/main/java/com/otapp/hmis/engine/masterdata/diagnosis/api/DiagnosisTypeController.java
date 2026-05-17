package com.otapp.hmis.engine.masterdata.diagnosis.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.diagnosis.application.DiagnosisTypeDtos.CreateDiagnosisTypeRequest;
import com.otapp.hmis.engine.masterdata.diagnosis.application.DiagnosisTypeDtos.DiagnosisTypeDto;
import com.otapp.hmis.engine.masterdata.diagnosis.application.DiagnosisTypeDtos.UpdateDiagnosisTypeRequest;
import com.otapp.hmis.engine.masterdata.diagnosis.application.DiagnosisTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Diagnoses")
@RestController
@RequestMapping("/masterdata/diagnoses")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class DiagnosisTypeController {

    private final DiagnosisTypeService service;

    @PostMapping
    public ResponseEntity<DiagnosisTypeDto> create(@Valid @RequestBody CreateDiagnosisTypeRequest request) {
        DiagnosisTypeDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/diagnoses/uid/{diagnosisTypeUid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<DiagnosisTypeDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{diagnosisTypeUid}")
    public ResponseEntity<DiagnosisTypeDto> findByUid(@PathVariable String diagnosisTypeUid) {
        return ResponseEntity.ok(service.findByUid(diagnosisTypeUid));
    }

    @PutMapping("/uid/{diagnosisTypeUid}")
    public ResponseEntity<DiagnosisTypeDto> update(@PathVariable String diagnosisTypeUid, @Valid @RequestBody UpdateDiagnosisTypeRequest request) {
        return ResponseEntity.ok(service.update(diagnosisTypeUid, request));
    }

    @PutMapping("/uid/{diagnosisTypeUid}/active")
    public ResponseEntity<DiagnosisTypeDto> setActive(@PathVariable String diagnosisTypeUid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(diagnosisTypeUid, request.active()));
    }

    @DeleteMapping("/uid/{diagnosisTypeUid}")
    public ResponseEntity<Void> delete(@PathVariable String diagnosisTypeUid) {
        service.delete(diagnosisTypeUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
