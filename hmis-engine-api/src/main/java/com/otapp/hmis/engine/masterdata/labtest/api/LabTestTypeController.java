package com.otapp.hmis.engine.masterdata.labtest.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestTypeDtos.CreateLabTestTypeRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestTypeDtos.LabTestTypeDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestTypeDtos.UpdateLabTestTypeRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Lab tests")
@RestController
@RequestMapping("/masterdata/lab-tests")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class LabTestTypeController {

    private final LabTestTypeService service;

    @PostMapping
    public ResponseEntity<LabTestTypeDto> create(@Valid @RequestBody CreateLabTestTypeRequest request) {
        LabTestTypeDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/lab-tests/uid/{labTestUid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<LabTestTypeDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{labTestUid}")
    public ResponseEntity<LabTestTypeDto> findByUid(@PathVariable String labTestUid) {
        return ResponseEntity.ok(service.findByUid(labTestUid));
    }

    @PutMapping("/uid/{labTestUid}")
    public ResponseEntity<LabTestTypeDto> update(@PathVariable String labTestUid, @Valid @RequestBody UpdateLabTestTypeRequest request) {
        return ResponseEntity.ok(service.update(labTestUid, request));
    }

    @PutMapping("/uid/{labTestUid}/active")
    public ResponseEntity<LabTestTypeDto> setActive(@PathVariable String labTestUid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(labTestUid, request.active()));
    }

    @DeleteMapping("/uid/{labTestUid}")
    public ResponseEntity<Void> delete(@PathVariable String labTestUid) {
        service.delete(labTestUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
