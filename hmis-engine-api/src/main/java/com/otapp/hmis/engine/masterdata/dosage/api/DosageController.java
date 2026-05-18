package com.otapp.hmis.engine.masterdata.dosage.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.dosage.application.DosageDtos.CreateDosageRequest;
import com.otapp.hmis.engine.masterdata.dosage.application.DosageDtos.DosageDto;
import com.otapp.hmis.engine.masterdata.dosage.application.DosageDtos.UpdateDosageRequest;
import com.otapp.hmis.engine.masterdata.dosage.application.DosageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Dosages")
@RestController
@RequestMapping("/masterdata/dosages")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class DosageController {

    private final DosageService service;

    @PostMapping
    public ResponseEntity<DosageDto> create(@Valid @RequestBody CreateDosageRequest request) {
        DosageDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/dosages/uid/{dosageUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<DosageDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{dosageUid}")
    public ResponseEntity<DosageDto> findByUid(@PathVariable String dosageUid) {
        return ResponseEntity.ok(service.findByUid(dosageUid));
    }

    @PutMapping("/uid/{dosageUid}")
    public ResponseEntity<DosageDto> update(@PathVariable String dosageUid,
                                            @Valid @RequestBody UpdateDosageRequest request) {
        return ResponseEntity.ok(service.update(dosageUid, request));
    }

    @PutMapping("/uid/{dosageUid}/active")
    public ResponseEntity<DosageDto> setActive(@PathVariable String dosageUid,
                                               @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(dosageUid, request.active()));
    }

    @DeleteMapping("/uid/{dosageUid}")
    public ResponseEntity<Void> delete(@PathVariable String dosageUid) {
        service.delete(dosageUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
