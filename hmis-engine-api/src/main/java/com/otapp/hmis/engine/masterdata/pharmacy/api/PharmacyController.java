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
        URI loc = UriComponentsBuilder.fromPath("/masterdata/pharmacies/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PharmacyDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(pharmacyService.search(query, active, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<PharmacyDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(pharmacyService.findByUid(uid));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<PharmacyDto> update(@PathVariable String uid, @Valid @RequestBody UpdatePharmacyRequest request) {
        return ResponseEntity.ok(pharmacyService.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<PharmacyDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(pharmacyService.setActive(uid, request.active()));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        pharmacyService.delete(uid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
