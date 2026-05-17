package com.otapp.hmis.engine.masterdata.ward.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.ward.application.WardService;
import com.otapp.hmis.engine.masterdata.ward.application.dto.CreateWardRequest;
import com.otapp.hmis.engine.masterdata.ward.application.dto.UpdateWardRequest;
import com.otapp.hmis.engine.masterdata.ward.application.dto.WardDto;
import com.otapp.hmis.engine.masterdata.ward.domain.WardCategory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Wards")
@RestController
@RequestMapping("/masterdata/wards")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class WardController {

    private final WardService wardService;

    @PostMapping
    public ResponseEntity<WardDto> create(@Valid @RequestBody CreateWardRequest request) {
        WardDto created = wardService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/wards/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<WardDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) WardCategory category,
            Pageable pageable) {
        return ResponseEntity.ok(wardService.search(query, active, category, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<WardDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(wardService.findByUid(uid));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<WardDto> update(@PathVariable String uid, @Valid @RequestBody UpdateWardRequest request) {
        return ResponseEntity.ok(wardService.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<WardDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(wardService.setActive(uid, request.active()));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        wardService.delete(uid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
