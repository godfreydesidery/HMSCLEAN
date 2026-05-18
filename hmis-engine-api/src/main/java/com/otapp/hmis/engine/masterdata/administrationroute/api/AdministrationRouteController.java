package com.otapp.hmis.engine.masterdata.administrationroute.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.administrationroute.application.AdministrationRouteDtos.AdministrationRouteDto;
import com.otapp.hmis.engine.masterdata.administrationroute.application.AdministrationRouteDtos.CreateAdministrationRouteRequest;
import com.otapp.hmis.engine.masterdata.administrationroute.application.AdministrationRouteDtos.UpdateAdministrationRouteRequest;
import com.otapp.hmis.engine.masterdata.administrationroute.application.AdministrationRouteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Administration routes")
@RestController
@RequestMapping("/masterdata/administration-routes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class AdministrationRouteController {

    private final AdministrationRouteService service;

    @PostMapping
    public ResponseEntity<AdministrationRouteDto> create(
            @Valid @RequestBody CreateAdministrationRouteRequest request) {
        AdministrationRouteDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/administration-routes/uid/{routeUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<AdministrationRouteDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{routeUid}")
    public ResponseEntity<AdministrationRouteDto> findByUid(@PathVariable String routeUid) {
        return ResponseEntity.ok(service.findByUid(routeUid));
    }

    @PutMapping("/uid/{routeUid}")
    public ResponseEntity<AdministrationRouteDto> update(@PathVariable String routeUid,
                                                         @Valid @RequestBody UpdateAdministrationRouteRequest request) {
        return ResponseEntity.ok(service.update(routeUid, request));
    }

    @PutMapping("/uid/{routeUid}/active")
    public ResponseEntity<AdministrationRouteDto> setActive(@PathVariable String routeUid,
                                                            @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(routeUid, request.active()));
    }

    @DeleteMapping("/uid/{routeUid}")
    public ResponseEntity<Void> delete(@PathVariable String routeUid) {
        service.delete(routeUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
