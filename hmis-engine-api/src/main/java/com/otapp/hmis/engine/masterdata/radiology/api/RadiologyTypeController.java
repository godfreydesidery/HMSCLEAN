package com.otapp.hmis.engine.masterdata.radiology.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.radiology.application.RadiologyTypeDtos.CreateRadiologyTypeRequest;
import com.otapp.hmis.engine.masterdata.radiology.application.RadiologyTypeDtos.RadiologyTypeDto;
import com.otapp.hmis.engine.masterdata.radiology.application.RadiologyTypeDtos.UpdateRadiologyTypeRequest;
import com.otapp.hmis.engine.masterdata.radiology.application.RadiologyTypeService;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyModality;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Radiology")
@RestController
@RequestMapping("/masterdata/radiology")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class RadiologyTypeController {

    private final RadiologyTypeService service;

    @PostMapping
    public ResponseEntity<RadiologyTypeDto> create(@Valid @RequestBody CreateRadiologyTypeRequest request) {
        RadiologyTypeDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/radiology/uid/{radiologyUid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<RadiologyTypeDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) RadiologyModality modality,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, modality, pageable));
    }

    @GetMapping("/uid/{radiologyUid}")
    public ResponseEntity<RadiologyTypeDto> findByUid(@PathVariable String radiologyUid) {
        return ResponseEntity.ok(service.findByUid(radiologyUid));
    }

    @PutMapping("/uid/{radiologyUid}")
    public ResponseEntity<RadiologyTypeDto> update(@PathVariable String radiologyUid, @Valid @RequestBody UpdateRadiologyTypeRequest request) {
        return ResponseEntity.ok(service.update(radiologyUid, request));
    }

    @PutMapping("/uid/{radiologyUid}/active")
    public ResponseEntity<RadiologyTypeDto> setActive(@PathVariable String radiologyUid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(radiologyUid, request.active()));
    }

    @DeleteMapping("/uid/{radiologyUid}")
    public ResponseEntity<Void> delete(@PathVariable String radiologyUid) {
        service.delete(radiologyUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
