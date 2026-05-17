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
