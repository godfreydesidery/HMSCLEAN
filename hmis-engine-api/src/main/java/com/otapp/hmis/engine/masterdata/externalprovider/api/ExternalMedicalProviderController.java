package com.otapp.hmis.engine.masterdata.externalprovider.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.externalprovider.application.ExternalMedicalProviderDtos.CreateExternalMedicalProviderRequest;
import com.otapp.hmis.engine.masterdata.externalprovider.application.ExternalMedicalProviderDtos.ExternalMedicalProviderDto;
import com.otapp.hmis.engine.masterdata.externalprovider.application.ExternalMedicalProviderDtos.UpdateExternalMedicalProviderRequest;
import com.otapp.hmis.engine.masterdata.externalprovider.application.ExternalMedicalProviderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "External medical providers")
@RestController
@RequestMapping("/masterdata/external-providers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class ExternalMedicalProviderController {

    private final ExternalMedicalProviderService service;

    @PostMapping
    public ResponseEntity<ExternalMedicalProviderDto> create(@Valid @RequestBody CreateExternalMedicalProviderRequest request) {
        ExternalMedicalProviderDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/external-providers/uid/{externalProviderUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    /**
     * Listing is also reachable by clinicians (ENCOUNTER_ACCESS) so the referral
     * closure form can offer a provider picker without granting masterdata rights.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('MASTERDATA_MANAGE','ENCOUNTER_ACCESS')")
    public ResponseEntity<PageResponse<ExternalMedicalProviderDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{externalProviderUid}")
    @PreAuthorize("hasAnyAuthority('MASTERDATA_MANAGE','ENCOUNTER_ACCESS')")
    public ResponseEntity<ExternalMedicalProviderDto> findByUid(@PathVariable String externalProviderUid) {
        return ResponseEntity.ok(service.findByUid(externalProviderUid));
    }

    @PutMapping("/uid/{externalProviderUid}")
    public ResponseEntity<ExternalMedicalProviderDto> update(@PathVariable String externalProviderUid,
                                                             @Valid @RequestBody UpdateExternalMedicalProviderRequest request) {
        return ResponseEntity.ok(service.update(externalProviderUid, request));
    }

    @PutMapping("/uid/{externalProviderUid}/active")
    public ResponseEntity<ExternalMedicalProviderDto> setActive(@PathVariable String externalProviderUid,
                                                                @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(externalProviderUid, request.active()));
    }

    @DeleteMapping("/uid/{externalProviderUid}")
    public ResponseEntity<Void> delete(@PathVariable String externalProviderUid) {
        service.delete(externalProviderUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
