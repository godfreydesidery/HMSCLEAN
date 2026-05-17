package com.otapp.hmis.engine.masterdata.store.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.CreateStoreRequest;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.StoreDto;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.UpdateStoreRequest;
import com.otapp.hmis.engine.masterdata.store.application.StoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Stores")
@RestController
@RequestMapping("/masterdata/stores")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<StoreDto> create(@Valid @RequestBody CreateStoreRequest request) {
        StoreDto created = storeService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/stores/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<StoreDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(storeService.search(query, active, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<StoreDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(storeService.findByUid(uid));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<StoreDto> update(@PathVariable String uid, @Valid @RequestBody UpdateStoreRequest request) {
        return ResponseEntity.ok(storeService.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<StoreDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(storeService.setActive(uid, request.active()));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        storeService.delete(uid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
