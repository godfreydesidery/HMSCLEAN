package com.otapp.hmis.engine.masterdata.bed.api;

import com.otapp.hmis.engine.masterdata.bed.application.BedDtos.BedDto;
import com.otapp.hmis.engine.masterdata.bed.application.BedDtos.CreateBedRequest;
import com.otapp.hmis.engine.masterdata.bed.application.BedDtos.OutOfServiceRequest;
import com.otapp.hmis.engine.masterdata.bed.application.BedDtos.UpdateBedRequest;
import com.otapp.hmis.engine.masterdata.bed.application.BedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Beds")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class BedController {

    private final BedService service;

    @GetMapping("/masterdata/wards/uid/{wardUid}/beds")
    public ResponseEntity<List<BedDto>> listForWard(@PathVariable String wardUid) {
        return ResponseEntity.ok(service.listForWard(wardUid));
    }

    @PostMapping("/masterdata/wards/uid/{wardUid}/beds")
    public ResponseEntity<BedDto> create(@PathVariable String wardUid,
                                         @Valid @RequestBody CreateBedRequest request) {
        BedDto created = service.create(wardUid, request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/beds/uid/{bedUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping("/masterdata/beds/uid/{bedUid}")
    public ResponseEntity<BedDto> findByUid(@PathVariable String bedUid) {
        return ResponseEntity.ok(service.findByUid(bedUid));
    }

    @PutMapping("/masterdata/beds/uid/{bedUid}")
    public ResponseEntity<BedDto> update(@PathVariable String bedUid,
                                         @Valid @RequestBody UpdateBedRequest request) {
        return ResponseEntity.ok(service.update(bedUid, request));
    }

    @PutMapping("/masterdata/beds/uid/{bedUid}/active")
    public ResponseEntity<BedDto> setActive(@PathVariable String bedUid,
                                            @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(bedUid, request.active()));
    }

    @PostMapping("/masterdata/beds/uid/{bedUid}/out-of-service")
    public ResponseEntity<BedDto> outOfService(@PathVariable String bedUid,
                                               @Valid @RequestBody(required = false) OutOfServiceRequest request) {
        return ResponseEntity.ok(service.markOutOfService(bedUid, request));
    }

    @PostMapping("/masterdata/beds/uid/{bedUid}/free")
    public ResponseEntity<BedDto> markFree(@PathVariable String bedUid) {
        return ResponseEntity.ok(service.markFree(bedUid));
    }

    @DeleteMapping("/masterdata/beds/uid/{bedUid}")
    public ResponseEntity<Void> delete(@PathVariable String bedUid) {
        service.delete(bedUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
