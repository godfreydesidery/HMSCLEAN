package com.otapp.hmis.engine.masterdata.insurance.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.insurance.application.InsurancePlanDtos.CreateInsurancePlanRequest;
import com.otapp.hmis.engine.masterdata.insurance.application.InsurancePlanDtos.InsurancePlanDto;
import com.otapp.hmis.engine.masterdata.insurance.application.InsurancePlanDtos.UpdateInsurancePlanRequest;
import com.otapp.hmis.engine.masterdata.insurance.application.InsurancePlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Insurance plans")
@RestController
@RequestMapping("/masterdata/insurance-plans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class InsurancePlanController {

    private final InsurancePlanService service;

    @PostMapping
    public ResponseEntity<InsurancePlanDto> create(@Valid @RequestBody CreateInsurancePlanRequest request) {
        InsurancePlanDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/insurance-plans/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<InsurancePlanDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String providerUid,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, providerUid, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<InsurancePlanDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(service.findByUid(uid));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<InsurancePlanDto> update(@PathVariable String uid, @Valid @RequestBody UpdateInsurancePlanRequest request) {
        return ResponseEntity.ok(service.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<InsurancePlanDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(uid, request.active()));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        service.delete(uid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
