package com.otapp.hmis.engine.masterdata.store.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.CreateStoreRequest;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.StoreDto;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.UpdateStoreRequest;
import com.otapp.hmis.engine.masterdata.store.application.StoreService;
import com.otapp.hmis.engine.masterdata.store.application.StoreStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
    private final StoreStaffService storeStaffService;

    /**
     * The active stores the calling storekeeper is affiliated with (legacy
     * {@code load_stores_by_store_person}). Scoped to {@code StoreStaff} so a
     * keeper's store picker only offers stores they may operate; an unaffiliated
     * user (e.g. ROOT) gets an empty list. Gated on {@code STORE_ACCESS} — the
     * privilege the store-operating endpoints use — overriding the master-data
     * management gate on this controller.
     */
    @Operation(summary = "List the stores the current storekeeper is affiliated with")
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('STORE_ACCESS')")
    public ResponseEntity<List<StoreDto>> mine() {
        return ResponseEntity.ok(storeStaffService.listMyStores());
    }

    @PostMapping
    public ResponseEntity<StoreDto> create(@Valid @RequestBody CreateStoreRequest request) {
        StoreDto created = storeService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/stores/uid/{storeUid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<StoreDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(storeService.search(query, active, pageable));
    }

    @GetMapping("/uid/{storeUid}")
    public ResponseEntity<StoreDto> findByUid(@PathVariable String storeUid) {
        return ResponseEntity.ok(storeService.findByUid(storeUid));
    }

    @PutMapping("/uid/{storeUid}")
    public ResponseEntity<StoreDto> update(@PathVariable String storeUid, @Valid @RequestBody UpdateStoreRequest request) {
        return ResponseEntity.ok(storeService.update(storeUid, request));
    }

    @PutMapping("/uid/{storeUid}/active")
    public ResponseEntity<StoreDto> setActive(@PathVariable String storeUid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(storeService.setActive(storeUid, request.active()));
    }

    @DeleteMapping("/uid/{storeUid}")
    public ResponseEntity<Void> delete(@PathVariable String storeUid) {
        storeService.delete(storeUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
