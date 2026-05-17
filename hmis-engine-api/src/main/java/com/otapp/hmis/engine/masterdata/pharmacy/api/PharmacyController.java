package com.otapp.hmis.engine.masterdata.pharmacy.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.pharmacy.application.PharmacyDtos.CreatePharmacyRequest;
import com.otapp.hmis.engine.masterdata.pharmacy.application.PharmacyDtos.PharmacyDto;
import com.otapp.hmis.engine.masterdata.pharmacy.application.PharmacyDtos.UpdatePharmacyRequest;
import com.otapp.hmis.engine.masterdata.pharmacy.application.PharmacyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Pharmacies")
@RestController
@RequestMapping("/masterdata/pharmacies")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @PostMapping
    public ResponseEntity<PharmacyDto> create(@Valid @RequestBody CreatePharmacyRequest request) {
        PharmacyDto created = pharmacyService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/pharmacies/uid/{pharmacyUid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PharmacyDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(pharmacyService.search(query, active, pageable));
    }

    @GetMapping("/uid/{pharmacyUid}")
    public ResponseEntity<PharmacyDto> findByUid(@PathVariable String pharmacyUid) {
        return ResponseEntity.ok(pharmacyService.findByUid(pharmacyUid));
    }

    @PutMapping("/uid/{pharmacyUid}")
    public ResponseEntity<PharmacyDto> update(@PathVariable String pharmacyUid, @Valid @RequestBody UpdatePharmacyRequest request) {
        return ResponseEntity.ok(pharmacyService.update(pharmacyUid, request));
    }

    @PutMapping("/uid/{pharmacyUid}/active")
    public ResponseEntity<PharmacyDto> setActive(@PathVariable String pharmacyUid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(pharmacyService.setActive(pharmacyUid, request.active()));
    }

    @DeleteMapping("/uid/{pharmacyUid}")
    public ResponseEntity<Void> delete(@PathVariable String pharmacyUid) {
        pharmacyService.delete(pharmacyUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
