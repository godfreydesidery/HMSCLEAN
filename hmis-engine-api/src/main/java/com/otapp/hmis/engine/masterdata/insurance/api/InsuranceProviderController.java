package com.otapp.hmis.engine.masterdata.insurance.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.insurance.application.InsuranceProviderDtos.CreateInsuranceProviderRequest;
import com.otapp.hmis.engine.masterdata.insurance.application.InsuranceProviderDtos.InsuranceProviderDto;
import com.otapp.hmis.engine.masterdata.insurance.application.InsuranceProviderDtos.UpdateInsuranceProviderRequest;
import com.otapp.hmis.engine.masterdata.insurance.application.InsuranceProviderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Insurance providers")
@RestController
@RequestMapping("/masterdata/insurance-providers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class InsuranceProviderController {

    private final InsuranceProviderService service;

    @PostMapping
    public ResponseEntity<InsuranceProviderDto> create(@Valid @RequestBody CreateInsuranceProviderRequest request) {
        InsuranceProviderDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/insurance-providers/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<InsuranceProviderDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<InsuranceProviderDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(service.findByUid(uid));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<InsuranceProviderDto> update(@PathVariable String uid, @Valid @RequestBody UpdateInsuranceProviderRequest request) {
        return ResponseEntity.ok(service.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<InsuranceProviderDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(uid, request.active()));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        service.delete(uid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
